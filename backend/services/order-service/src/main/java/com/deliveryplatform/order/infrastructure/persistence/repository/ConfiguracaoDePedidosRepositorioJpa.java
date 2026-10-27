package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.application.port.out.ConfiguracaoDePedidosRepositorio;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.infrastructure.persistence.entity.ConfiguracaoDePedidosJpaEntity;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ConfiguracaoDePedidosRepositorioJpa implements ConfiguracaoDePedidosRepositorio {

    private final ConfiguracaoSpringDataRepository springData;

    public ConfiguracaoDePedidosRepositorioJpa(ConfiguracaoSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public Optional<ConfiguracaoDePedidos> daLoja(UUID estabelecimentoId) {
        return springData.findById(estabelecimentoId)
                .map(e -> ConfiguracaoDePedidos.reconstituir(e.getEstabelecimentoId(),
                        Money.de(e.getTaxaDeEntrega()), e.isAceitaEntrega(), e.isAceitaRetirada()));
    }

    @Override
    public ConfiguracaoDePedidos salvar(ConfiguracaoDePedidos c) {
        // Chave atribuída e sem versão: o save faz merge, que insere ou atualiza.
        springData.save(new ConfiguracaoDePedidosJpaEntity(c.getEstabelecimentoId(),
                c.getTaxaDeEntrega().valor(), c.isAceitaEntrega(), c.isAceitaRetirada()));
        return c;
    }
}
