package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.exception.AcessoNegado;
import com.deliveryplatform.merchant.application.port.in.ConsultarHorario;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.domain.model.Disponibilidade;
import com.deliveryplatform.merchant.domain.model.Permissao;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * A irmã de leitura do {@code DefinirHorarioService} (ADR-073, emenda de 17/10/2026).
 *
 * <p><b>Autoriza antes de buscar</b>, pela mesma porta e com a mesma permissão da escrita:
 * sem vínculo ativo com {@code CONFIGURAR_LOJA} é o 403 único (M7), loja que não existe
 * inclusive (ADR-056).
 *
 * <p><b>Loja que nunca abre por horário devolve o {@code semHorario()}</b> — o estado
 * nomeado, que a rota responde como mapa vazio com 200. Vazio é um horário, e não a
 * ausência de um: a tela precisa distinguir "não abre por horário" de "não consegui ler".
 */
@Service
public class ConsultarHorarioService implements ConsultarHorario {

    private final MembroRepositorio membros;
    private final EstabelecimentoRepositorio estabelecimentos;

    public ConsultarHorarioService(MembroRepositorio membros,
                                   EstabelecimentoRepositorio estabelecimentos) {
        this.membros = membros;
        this.estabelecimentos = estabelecimentos;
    }

    @Override
    public Disponibilidade daLoja(UUID lojaId, UUID autorUsuarioId) {
        membros.buscarPorUsuarioELoja(autorUsuarioId, lojaId)
                .filter(vinculo -> vinculo.pode(Permissao.CONFIGURAR_LOJA))
                .orElseThrow(AcessoNegado::new);

        return estabelecimentos.buscarPorId(lojaId)
                .orElseThrow(AcessoNegado::new)
                .getDisponibilidade();
    }
}
