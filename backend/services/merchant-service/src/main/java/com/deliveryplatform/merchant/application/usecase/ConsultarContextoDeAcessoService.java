package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.port.in.ConsultarContextoDeAcesso;
import com.deliveryplatform.merchant.application.port.in.ContextoDeAcesso;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Membro;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Cinco linhas, e uma delas é a que não se escreve por descuido.
 *
 * <p><b>O {@code filter(Membro::ativo)} é obrigatório.</b> O
 * {@link MembroRepositorio#buscarPorUsuarioELoja} devolve o vínculo em
 * <b>qualquer</b> estado — o adaptador faz
 * {@code findByUsuarioIdAndEstabelecimentoId} sem filtrar —, então um vínculo
 * {@code SUSPENSO} ou {@code REMOVIDO} chega aqui preenchido. O
 * {@link ConsultarEquipeService} sobrevive a isso sem filtrar porque o
 * {@code Membro.pode(...)} confere {@code ativo()} por dentro; <b>este caso de
 * uso não chama {@code pode(...)}</b>, porque não exige permissão nenhuma.
 *
 * <p>Sem essa linha, um funcionário demitido continuaria recebendo o contexto
 * de acesso com todas as permissões dele, e cada serviço que perguntasse
 * autorizaria a requisição. Seria a falha mais silenciosa que este sistema
 * consegue ter: um teste de caminho feliz passa, o cardápio abre, e a demissão
 * não surte efeito em lugar nenhum.
 *
 * <p><b>Não há cache aqui, e não vai haver.</b> A ADR-043 emendou a ADR-011
 * nesse ponto: o {@code merchant} é a fonte da verdade do vínculo — lê a própria
 * tabela — e um cache aqui serviria para o serviço não perguntar a si mesmo. O
 * cache de 60 s/10 s mora em <b>cada serviço que pergunta</b>, alimentado pelo
 * {@code VinculoAlteradoV1}. O primeiro é o {@code catalog}, na G-B3.
 *
 * <p><b>E as permissões saem como estão gravadas.</b> Nenhuma dedução a partir
 * do papel: {@code estabelecimento.md} §2 diz que papel não é lista de
 * permissões, e o {@code promover} do agregado muda só o papel.
 */
@Service
public class ConsultarContextoDeAcessoService implements ConsultarContextoDeAcesso {

    private final MembroRepositorio membros;

    public ConsultarContextoDeAcessoService(MembroRepositorio membros) {
        this.membros = membros;
    }

    @Override
    public ContextoDeAcesso doPortador(UUID estabelecimentoId, UUID usuarioAutenticado) {
        Membro vinculo = membros
                .buscarPorUsuarioELoja(usuarioAutenticado, estabelecimentoId)
                .filter(Membro::ativo)
                .orElseThrow(AcessoNegado::new);

        return new ContextoDeAcesso(
                vinculo.getUsuarioId(),
                vinculo.getEstabelecimentoId(),
                vinculo.getPapel(),
                vinculo.getPermissoes());
    }
}
