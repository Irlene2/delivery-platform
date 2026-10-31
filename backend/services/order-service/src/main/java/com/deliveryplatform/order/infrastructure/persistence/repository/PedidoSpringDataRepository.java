package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.order.infrastructure.persistence.entity.PedidoJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface PedidoSpringDataRepository extends JpaRepository<PedidoJpaEntity, UUID> {

    Optional<PedidoJpaEntity> findByCodigoDeAcompanhamento(UUID codigo);

    List<PedidoJpaEntity> findByEstabelecimentoIdAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
            UUID estabelecimentoId, Instant desde, Instant ate, Pageable teto);

    List<PedidoJpaEntity> findByEstabelecimentoIdAndStatusAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
            UUID estabelecimentoId, StatusDoPedido status, Instant desde, Instant ate, Pageable teto);

    List<PedidoJpaEntity> findByEstabelecimentoIdAndEntregadorIdAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
            UUID estabelecimentoId, UUID entregadorId, Instant desde, Instant ate, Pageable teto);
}
