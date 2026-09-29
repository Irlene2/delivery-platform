package com.deliveryplatform.catalog.infrastructure.cache;

import com.deliveryplatform.catalog.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.catalog.config.CacheDaAutorizacaoProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

import java.util.concurrent.atomic.AtomicLong;

/**
 * O cache de autorização — o armazenamento, separado de quem o usa e de quem o
 * invalida.
 *
 * <p>Três donos distintos mexem nele, e é por isso que ele é uma classe e não um
 * campo: o {@code AutorizacaoComercialEmCache} lê e escreve, o
 * {@code OuvinteDeVinculoAlterado} remove, e o ouvinte de conexão esvazia. Um
 * {@code Caffeine} escondido dentro do decorador obrigaria os outros dois a
 * conhecê-lo.
 *
 * <h2>Dois prazos, um cache</h2>
 *
 * <p>A ADR-011 pede <b>60 s</b> para resposta positiva e <b>10 s</b> para
 * negativa. Um {@link Expiry} decide por entrada: vazio expira antes porque
 * <i>"acesso recém-concedido não pode demorar"</i> — quem acabou de ser
 * convidado não pode esperar um minuto para o sistema notar.
 *
 * <p><b>Indisponibilidade nunca é guardada.</b> A {@code AutorizacaoIndisponivel}
 * sobe do adaptador e não chega aqui: só se chama {@link #guardar} com o que o
 * {@code merchant} de fato respondeu. Cachear "não respondeu" como se fosse "não
 * tem acesso" transformaria uma queda de dois segundos em dez segundos de recusa
 * para todos.
 *
 * <h2>A geração, e a corrida que ela fecha</h2>
 *
 * <p>Sem ela existe esta sequência, e ela não é hipotética:
 *
 * <pre>
 * t0  requisição A não acha no cache e pergunta ao merchant
 * t1  Marli revoga o acesso; o evento chega; a entrada é removida
 * t2  a resposta de A chega — com o contexto de ANTES — e é guardada
 * </pre>
 *
 * <p>O resultado é uma permissão revogada guardada <b>depois</b> da invalidação,
 * válida por 60 s. O evento chegou, foi aplicado, e não adiantou nada.
 *
 * <p>A defesa é um contador: quem lê anota a geração antes de perguntar e só
 * guarda se ela não mudou. <b>Qualquer</b> invalidação incrementa o contador, e
 * portanto descarta toda leitura em voo — inclusive as de outras chaves. É
 * exagerado de propósito: o custo de descartar uma leitura boa é uma consulta a
 * mais ao {@code merchant}; o custo de guardar uma ruim é acesso indevido.
 */
@Component
public class CacheDaAutorizacao {

    private static final Logger log = LoggerFactory.getLogger(CacheDaAutorizacao.class);

    private final Cache<ChaveDeAutorizacao, Optional<ContextoDeAcesso>> entradas;
    private final AtomicLong geracao = new AtomicLong();

    public CacheDaAutorizacao(CacheDaAutorizacaoProperties propriedades) {
        this.entradas = Caffeine.newBuilder()
                .maximumSize(propriedades.maximoDeEntradas())
                .expireAfter(new PrazoPorResposta(
                        propriedades.prazoPositivo().toNanos(),
                        propriedades.prazoNegativo().toNanos()))
                .build();
    }

    /** A geração corrente. Quem vai perguntar ao {@code merchant} anota isto antes. */
    public long geracao() {
        return geracao.get();
    }

    public Optional<Optional<ContextoDeAcesso>> procurar(ChaveDeAutorizacao chave) {
        return Optional.ofNullable(entradas.getIfPresent(chave));
    }

    /**
     * Guarda, <b>se nada foi invalidado desde que a leitura começou</b>.
     *
     * @param geracaoDaLeitura o valor de {@link #geracao()} lido antes da consulta
     * @return se guardou — só para o teste poder afirmar a corrida sem dormir
     */
    public boolean guardar(ChaveDeAutorizacao chave,
                           Optional<ContextoDeAcesso> resposta,
                           long geracaoDaLeitura) {
        if (geracao.get() != geracaoDaLeitura) {
            log.debug("resposta descartada: o cache foi invalidado durante a consulta");
            return false;
        }
        entradas.put(chave, resposta);
        return true;
    }

    /** O caminho rápido: o evento disse que este vínculo mudou. */
    public void invalidar(ChaveDeAutorizacao chave) {
        entradas.invalidate(chave);
        geracao.incrementAndGet();
    }

    /**
     * O caminho seguro: esquecer tudo.
     *
     * <p>Usado quando não se consegue aplicar uma invalidação, e quando a conexão
     * com o broker se refaz — nos dois casos o que se sabe é que <b>pode haver</b>
     * entrada velha, e não qual. Esquecer custa algumas consultas ao
     * {@code merchant}; não esquecer custa acesso que já foi retirado.
     */
    public void esvaziar(String motivo) {
        long quantas = entradas.estimatedSize();
        entradas.invalidateAll();
        geracao.incrementAndGet();
        if (quantas > 0) {
            log.warn("cache de autorização esvaziado ({} entradas): {}", quantas, motivo);
        }
    }

    /** Só para teste: quantas entradas há agora. */
    public long tamanho() {
        entradas.cleanUp();
        return entradas.estimatedSize();
    }

    /**
     * 60 s para quem tem vínculo, 10 s para quem não tem.
     *
     * <p>Os três métodos são de criação, atualização e leitura. <b>A leitura
     * devolve o prazo que já estava correndo</b>: se ela renovasse, uma entrada
     * consultada a cada segundo nunca expiraria, e o prazo — que a ADR-011 chama
     * de <i>"a janela de tolerância"</i> — deixaria de existir para exatamente
     * os usuários mais ativos.
     */
    private record PrazoPorResposta(long positivoEmNanos, long negativoEmNanos)
            implements Expiry<ChaveDeAutorizacao, Optional<ContextoDeAcesso>> {

        private long prazo(Optional<ContextoDeAcesso> resposta) {
            return resposta.isPresent() ? positivoEmNanos : negativoEmNanos;
        }

        @Override
        public long expireAfterCreate(ChaveDeAutorizacao chave,
                                      Optional<ContextoDeAcesso> resposta,
                                      long agora) {
            return prazo(resposta);
        }

        @Override
        public long expireAfterUpdate(ChaveDeAutorizacao chave,
                                      Optional<ContextoDeAcesso> resposta,
                                      long agora,
                                      @SuppressWarnings("unused") long prazoQueFaltava) {
            return prazo(resposta);
        }

        @Override
        public long expireAfterRead(ChaveDeAutorizacao chave,
                                    Optional<ContextoDeAcesso> resposta,
                                    long agora,
                                    long prazoQueFaltava) {
            return prazoQueFaltava;
        }
    }
}
