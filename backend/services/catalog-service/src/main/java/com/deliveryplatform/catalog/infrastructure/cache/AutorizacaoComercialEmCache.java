package com.deliveryplatform.catalog.infrastructure.cache;

import com.deliveryplatform.catalog.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.infrastructure.client.AutorizacaoComercialHttp;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * O cache que a ADR-011 desenhou em agosto, finalmente ligado — e ele é um
 * <b>decorador</b> da porta, não um campo dentro do adaptador HTTP.
 *
 * <p>O caso de uso continua injetando {@code AutorizacaoComercialPort} e não
 * sabe que existe cache. O {@link AutorizacaoComercialHttp} continua sem saber
 * que alguém guarda o que ele responde. Cada peça faz uma coisa, e a de trocar
 * uma pela outra é a anotação {@link Primary}.
 *
 * <h2>Por que o cache só entra agora</h2>
 *
 * <p>A ADR-043: <i>"o cache sem o evento é um cache que não invalida, e um cache
 * de autorização que não invalida é uma falha de segurança com nome de
 * otimização"</i>. O consumidor do {@code VinculoAlteradoV1} nasce nesta mesma
 * rodada, e é ele quem torna estas linhas defensáveis.
 *
 * <h2>Três respostas, e só duas são guardadas</h2>
 *
 * <table>
 *   <tr><th>o que o {@code merchant} respondeu</th><th>o que se guarda</th><th>por quanto</th></tr>
 *   <tr><td>200 — há vínculo</td><td>o contexto</td><td>60 s</td></tr>
 *   <tr><td>403 — não há vínculo</td><td>o vazio</td><td>10 s</td></tr>
 *   <tr><td><b>não respondeu</b></td><td><b>nada</b></td><td>—</td></tr>
 * </table>
 *
 * <p>A terceira linha é a razão de a G-B3 ter separado {@code Optional.empty()}
 * de {@code AutorizacaoIndisponivel} uma rodada <i>antes</i> de existir cache.
 * A exceção atravessa este decorador sem tocar no armazenamento — não há
 * {@code catch} aqui, e a ausência é a decisão. Guardar "não respondeu" como se
 * fosse "não tem acesso" transformaria dois segundos de queda do
 * {@code merchant} em dez segundos de recusa para todo mundo.
 *
 * <h2>O portador vem do token, e a porta continua sem recebê-lo</h2>
 *
 * <p>A chave é o par {@code (usuarioId, estabelecimentoId)}, e a assinatura da
 * porta é {@code contexto(UUID estabelecimentoId)} — a ADR-045 tirou o usuário
 * de lá de propósito, porque o chamador não <i>afirma</i> quem é: ele
 * <i>prova</i>, encaminhando o token. Então o portador é lido aqui do mesmo
 * lugar de onde o adaptador HTTP lê o token, e pelo mesmo motivo.
 *
 * <p><b>Isto amarra o cache à requisição de usuário</b>, exatamente como o
 * adaptador já estava amarrado. Fora de uma requisição não há portador, não há
 * chave, e a chamada segue direto para a porta de baixo — que vai negar, porque
 * também não acha token. É a limitação da ADR-045 aparecendo no segundo lugar
 * que depende dela.
 */
@Component
@Primary
public class AutorizacaoComercialEmCache implements AutorizacaoComercialPort {

    private final AutorizacaoComercialHttp abaixo;
    private final CacheDaAutorizacao cache;

    /**
     * Recebe o tipo concreto, e não a interface.
     *
     * <p>Injetar {@code AutorizacaoComercialPort} aqui seria o Spring tentando
     * injetar este próprio bean — que é o {@link Primary} — em si mesmo. O
     * decorador precisa nomear quem ele decora, e nomear é o que impede o laço.
     */
    public AutorizacaoComercialEmCache(AutorizacaoComercialHttp abaixo, CacheDaAutorizacao cache) {
        this.abaixo = abaixo;
        this.cache = cache;
    }

    @Override
    public Optional<ContextoDeAcesso> contexto(UUID estabelecimentoId) {
        Optional<String> portador = portador();
        if (portador.isEmpty()) {
            // Sem token não há chave. Deixa a porta de baixo responder — ela
            // nega, e é ela que tem a mensagem certa para isso.
            return abaixo.contexto(estabelecimentoId);
        }

        ChaveDeAutorizacao chave = new ChaveDeAutorizacao(portador.get(), estabelecimentoId);

        Optional<Optional<ContextoDeAcesso>> guardado = cache.procurar(chave);
        if (guardado.isPresent()) {
            return guardado.get();
        }

        // A geração é lida ANTES da consulta. Entre esta linha e o `guardar`,
        // um evento pode invalidar o que se está buscando, e aí a resposta que
        // chegar já nasceu velha. Ver o javadoc do CacheDaAutorizacao.
        long geracao = cache.geracao();

        // Sem try/catch: AutorizacaoIndisponivel sobe e NÃO é guardada.
        Optional<ContextoDeAcesso> resposta = abaixo.contexto(estabelecimentoId);

        cache.guardar(chave, resposta, geracao);
        return resposta;
    }

    /**
     * O {@code sub}, cru.
     *
     * <p>Não converte para {@code UUID}: quem faz isso é o {@code merchant}
     * (ADR-038), e um {@code sub} que não seja {@code UUID} precisa chegar lá
     * para ser recusado — não estourar aqui. Ver {@link ChaveDeAutorizacao}.
     */
    private static Optional<String> portador() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacao instanceof JwtAuthenticationToken comJwt) {
            String sub = comJwt.getToken().getSubject();
            return sub == null || sub.isBlank() ? Optional.empty() : Optional.of(sub);
        }
        return Optional.empty();
    }
}
