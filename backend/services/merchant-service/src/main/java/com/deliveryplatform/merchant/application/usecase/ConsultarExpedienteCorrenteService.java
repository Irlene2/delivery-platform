package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.exception.SemExpedientePorHorario;
import com.deliveryplatform.merchant.application.port.in.ConsultarExpedienteCorrente;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.Membro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Confere o vínculo, carrega a loja, e pergunta ao agregado.
 *
 * <h2>A ordem das duas recusas, e por que ela importa</h2>
 *
 * <p>O vínculo é conferido <b>antes</b> de a loja ser carregada, e por isso não
 * existe 404 nesta rota: quem não tem vínculo recebe 403, exista a loja ou não.
 * Invertido, a diferença entre os dois códigos diria a qualquer portador de
 * token quais identificadores de loja existem neste sistema.
 *
 * <p>Vínculo ativo apontando para loja inexistente é <b>defeito de dados</b>, e
 * estoura 500 de propósito. Um 403 ali esconderia uma inconsistência de banco
 * atrás de uma recusa que parece normal, e ela ficaria escondida para sempre.
 *
 * <p>O filtro do estado é explícito, e este é o <b>terceiro</b> lugar do
 * repositório que o faz na mão: o {@code MembroRepositorio} devolve vínculo
 * suspenso e removido, e quem não passa por {@code Membro.pode(...)} fica sem
 * rede. Aqui não se passa por {@code pode(...)} porque <b>nenhuma permissão é
 * exigida</b> — o que se exige é estar na loja.
 *
 * <h2>A conta não mora aqui</h2>
 *
 * <p>Este serviço não conhece {@code DiaOperacional}, nem hora de corte, nem
 * faixa. Ele pergunta ao {@link Estabelecimento}, que é quem tem o fuso. Um caso
 * de uso que compusesse a conta seria o segundo lugar a errar na virada das
 * 04:00 — e a F.1 existiu porque havia um primeiro.
 */
@Service
public class ConsultarExpedienteCorrenteService implements ConsultarExpedienteCorrente {

    private final MembroRepositorio membros;
    private final EstabelecimentoRepositorio lojas;
    private final Clock relogio;

    public ConsultarExpedienteCorrenteService(MembroRepositorio membros,
                                              EstabelecimentoRepositorio lojas,
                                              Clock relogio) {
        this.membros = membros;
        this.lojas = lojas;
        this.relogio = relogio;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate de(UUID estabelecimentoId, UUID usuarioId) {
        Membro vinculo = membros.buscarPorUsuarioELoja(usuarioId, estabelecimentoId)
                .filter(Membro::ativo)
                .orElseThrow(AcessoNegado::new);

        // O identificador vem do VÍNCULO conferido, não da URL. Aqui os dois são
        // iguais por construção; o hábito é que evita o dia em que não forem.
        Estabelecimento loja = lojas.buscarPorId(vinculo.getEstabelecimentoId())
                .orElseThrow(() -> new IllegalStateException(
                        "vínculo ativo apontando para estabelecimento que não existe: "
                                + vinculo.getEstabelecimentoId()));

        return loja.expedienteParaCarimbo(Instant.now(relogio))
                .orElseThrow(SemExpedientePorHorario::new);
    }
}
