package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.infrastructure.persistence.entity.ConfiguracaoDePedidosJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ConfiguracaoSpringDataRepository extends JpaRepository<ConfiguracaoDePedidosJpaEntity, UUID> {
}
