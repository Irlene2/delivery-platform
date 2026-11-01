package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.infrastructure.persistence.entity.FechamentoJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface FechamentoSpringDataRepository extends JpaRepository<FechamentoJpaEntity, UUID> {

    Optional<FechamentoJpaEntity> findByEstabelecimentoIdAndDia(UUID estabelecimentoId, LocalDate dia);

    boolean existsByEstabelecimentoIdAndDia(UUID estabelecimentoId, LocalDate dia);

    List<FechamentoJpaEntity> findByEstabelecimentoIdOrderByDiaDesc(UUID estabelecimentoId,
                                                                   Pageable pagina);
}
