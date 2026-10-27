package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.application.port.out.CardapioPort;
import com.deliveryplatform.order.application.port.out.ConfiguracaoDePedidosRepositorio;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Dublês em memória das portas de saída — sem Mockito, para o teste ler como a regra. */
final class Dubles {

    private Dubles() {
    }

    static final class Pedidos implements PedidoRepositorio {
        final Map<UUID, Pedido> gravados = new LinkedHashMap<>();
        final Map<UUID, Long> numeracao = new HashMap<>();
        Instant ultimoDesde;
        Instant ultimoAte;

        @Override
        public Pedido salvar(Pedido pedido) {
            gravados.put(pedido.getId(), pedido);
            return pedido;
        }

        @Override
        public Optional<Pedido> buscarPorId(UUID id) {
            return Optional.ofNullable(gravados.get(id));
        }

        @Override
        public Optional<Pedido> buscarPorCodigo(UUID codigo) {
            return gravados.values().stream()
                    .filter(p -> p.getCodigoDeAcompanhamento().equals(codigo)).findFirst();
        }

        @Override
        public List<Pedido> daLoja(UUID loja, StatusDoPedido status, Instant desde, Instant ate,
                                   int limite) {
            ultimoDesde = desde;
            ultimoAte = ate;
            List<Pedido> achados = new ArrayList<>();
            for (Pedido p : gravados.values()) {
                if (p.getEstabelecimentoId().equals(loja)
                        && (status == null || p.getStatus() == status)
                        && !p.getCriadoEm().isBefore(desde) && p.getCriadoEm().isBefore(ate)) {
                    achados.add(p);
                }
            }
            achados.sort(Comparator.comparing(Pedido::getCriadoEm).reversed());
            return achados.stream().limit(limite).toList();
        }

        @Override
        public long proximoNumero(UUID loja) {
            return numeracao.merge(loja, 1L, Long::sum);
        }
    }

    static final class Configuracoes implements ConfiguracaoDePedidosRepositorio {
        final Map<UUID, ConfiguracaoDePedidos> gravadas = new HashMap<>();

        @Override
        public Optional<ConfiguracaoDePedidos> daLoja(UUID loja) {
            return Optional.ofNullable(gravadas.get(loja));
        }

        @Override
        public ConfiguracaoDePedidos salvar(ConfiguracaoDePedidos c) {
            gravadas.put(c.getEstabelecimentoId(), c);
            return c;
        }
    }

    static final class Cardapio implements CardapioPort {
        final Map<UUID, ProdutoDoCardapio> publicados = new HashMap<>();
        boolean foraDoAr;

        Cardapio com(ProdutoDoCardapio produto) {
            publicados.put(produto.id(), produto);
            return this;
        }

        @Override
        public Map<UUID, ProdutoDoCardapio> publicadosDe(UUID loja) {
            if (foraDoAr) {
                throw new CardapioIndisponivel("fora do ar", null);
            }
            return Map.copyOf(publicados);
        }
    }

    /** O merchant: responde por loja, com as permissões dadas; loja sem resposta = sem vínculo. */
    static final class Merchant implements AutorizacaoComercialPort {
        final Map<UUID, Set<PermissaoDoPedido>> vinculos = new HashMap<>();

        Merchant permite(UUID loja, PermissaoDoPedido... permissoes) {
            Set<PermissaoDoPedido> conjunto = EnumSet.noneOf(PermissaoDoPedido.class);
            conjunto.addAll(List.of(permissoes));
            vinculos.put(loja, conjunto);
            return this;
        }

        @Override
        public Optional<ContextoDeAcesso> contexto(UUID loja) {
            Set<PermissaoDoPedido> permissoes = vinculos.get(loja);
            return permissoes == null ? Optional.empty()
                    : Optional.of(new ContextoDeAcesso(UUID.randomUUID(), loja, permissoes));
        }
    }
}
