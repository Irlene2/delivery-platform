package com.deliveryplatform.order.application.port.out;

import com.deliveryplatform.order.domain.model.FechamentoDoDia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Um fechamento por loja e dia (a unicidade é também do banco). */
public interface FechamentoRepositorio {

    FechamentoDoDia salvar(FechamentoDoDia fechamento);

    Optional<FechamentoDoDia> doDia(UUID estabelecimentoId, LocalDate dia);

    boolean existe(UUID estabelecimentoId, LocalDate dia);

    /** Os mais recentes primeiro. */
    List<FechamentoDoDia> recentes(UUID estabelecimentoId, int limite);
}
