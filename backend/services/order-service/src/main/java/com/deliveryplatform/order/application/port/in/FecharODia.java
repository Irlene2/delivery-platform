package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.FechamentoDoDia;
import com.deliveryplatform.valuetypes.Money;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Fechar o caixa do dia e consultar os fechados (ADR-077, emenda de 01/11/2026). */
public interface FecharODia {

    /**
     * @param dia nulo = hoje, no fuso da loja
     * @param dinheiroContado opcional: a conferência do caixa
     */
    FechamentoDoDia fechar(UUID estabelecimentoId, LocalDate dia, Money dinheiroContado,
                           String observacao);

    FechamentoDoDia doDia(UUID estabelecimentoId, LocalDate dia);

    List<FechamentoDoDia> recentes(UUID estabelecimentoId);

    /**
     * As vendas de um período (02/11/2026): os dias fechados de {@code de} a {@code ate},
     * inclusive. No máximo {@code 366} dias; dia sem fechamento não entra.
     */
    List<FechamentoDoDia> doPeriodo(UUID estabelecimentoId, LocalDate de, LocalDate ate);
}
