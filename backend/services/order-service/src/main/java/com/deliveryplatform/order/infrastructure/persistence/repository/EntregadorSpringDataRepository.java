package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.infrastructure.persistence.entity.EntregadorJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface EntregadorSpringDataRepository
        extends JpaRepository<EntregadorJpaEntity, EntregadorJpaEntity.Chave> {

    List<EntregadorJpaEntity> findByChaveEstabelecimentoIdOrderByNomeAsc(UUID estabelecimentoId);
}
