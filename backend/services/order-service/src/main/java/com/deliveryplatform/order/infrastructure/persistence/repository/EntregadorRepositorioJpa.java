package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.application.port.out.EntregadorRepositorio;
import com.deliveryplatform.order.domain.model.EntregadorDaLoja;
import com.deliveryplatform.order.infrastructure.persistence.entity.EntregadorJpaEntity;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EntregadorRepositorioJpa implements EntregadorRepositorio {

    private final EntregadorSpringDataRepository springData;

    public EntregadorRepositorioJpa(EntregadorSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<EntregadorDaLoja> buscar(UUID estabelecimentoId, UUID usuarioId) {
        return springData.findById(new EntregadorJpaEntity.Chave(estabelecimentoId, usuarioId))
                .map(EntregadorRepositorioJpa::paraDominio);
    }

    @Override
    public List<EntregadorDaLoja> daLoja(UUID estabelecimentoId) {
        return springData.findByChaveEstabelecimentoIdOrderByNomeAsc(estabelecimentoId).stream()
                .map(EntregadorRepositorioJpa::paraDominio)
                .toList();
    }

    @Override
    public EntregadorDaLoja salvar(EntregadorDaLoja e) {
        // Chave atribuída e sem versão: o save faz merge, que insere ou troca o nome.
        springData.save(new EntregadorJpaEntity(e.estabelecimentoId(), e.usuarioId(), e.nome()));
        return e;
    }

    private static EntregadorDaLoja paraDominio(EntregadorJpaEntity e) {
        return new EntregadorDaLoja(e.getEstabelecimentoId(), e.getUsuarioId(), e.getNome());
    }
}
