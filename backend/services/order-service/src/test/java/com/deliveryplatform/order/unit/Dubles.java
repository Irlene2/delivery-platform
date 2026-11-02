package com.deliveryplatform.order.unit;

import com.deliveryplatform.order.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.order.application.port.out.CardapioIndisponivel;
import com.deliveryplatform.order.application.port.out.CardapioPort;
import com.deliveryplatform.order.application.port.out.ConfiguracaoDePedidosRepositorio;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.EntregadorRepositorio;
import com.deliveryplatform.order.application.port.out.FechamentoRepositorio;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.ProdutoDoCardapio;
import com.deliveryplatform.order.application.port.out.SituacaoDaLojaPort;
import com.deliveryplatform.order.application.port.out.SituacaoIndisponivel;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.order.domain.model.EntregadorDaLoja;
import com.deliveryplatform.order.domain.model.FechamentoDoDia;
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
        public List<Pedido> doEntregador(UUID loja, UUID usuarioId, Instant desde, Instant ate,
                                         int limite) {
            return daLoja(loja, null, desde, ate, limite).stream()
                    .filter(p -> p.estaCom(usuarioId))
                    .toList();
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

    /** Os fechamentos do dia, por loja e dia. */
    static final class Fechamentos implements FechamentoRepositorio {
        final Map<String, FechamentoDoDia> gravados = new LinkedHashMap<>();

        @Override
        public FechamentoDoDia salvar(FechamentoDoDia f) {
            gravados.put(f.getEstabelecimentoId() + "/" + f.getDia(), f);
            return f;
        }

        @Override
        public Optional<FechamentoDoDia> doDia(UUID loja, java.time.LocalDate dia) {
            return Optional.ofNullable(gravados.get(loja + "/" + dia));
        }

        @Override
        public boolean existe(UUID loja, java.time.LocalDate dia) {
            return gravados.containsKey(loja + "/" + dia);
        }

        @Override
        public List<FechamentoDoDia> entre(UUID loja, java.time.LocalDate de, java.time.LocalDate ate) {
            return gravados.values().stream()
                    .filter(f -> f.getEstabelecimentoId().equals(loja))
                    .filter(f -> !f.getDia().isBefore(de) && !f.getDia().isAfter(ate))
                    .sorted(Comparator.comparing(FechamentoDoDia::getDia).reversed())
                    .toList();
        }

        @Override
        public List<FechamentoDoDia> recentes(UUID loja, int limite) {
            return gravados.values().stream()
                    .filter(f -> f.getEstabelecimentoId().equals(loja))
                    .sorted(Comparator.comparing(FechamentoDoDia::getDia).reversed())
                    .limit(limite)
                    .toList();
        }
    }

    /** A trava do dia sobre os fechamentos dados, no fuso das lojas da demonstração. */
    static com.deliveryplatform.order.application.usecase.TravaDoDia trava(
            Fechamentos fechamentos, java.time.Clock relogio) {
        return new com.deliveryplatform.order.application.usecase.TravaDoDia(
                fechamentos, relogio, "America/Sao_Paulo");
    }

    /** A loja aberta, salvo quando o teste diz outra coisa. */
    static final class Lojas implements SituacaoDaLojaPort {
        SituacaoDaLoja situacao = SituacaoDaLoja.abertaAgora();
        boolean foraDoAr;

        @Override
        public SituacaoDaLoja daLoja(UUID loja) {
            if (foraDoAr) {
                throw new SituacaoIndisponivel("fora do ar", null);
            }
            return situacao;
        }
    }

    static final class Entregadores implements EntregadorRepositorio {
        final Map<String, EntregadorDaLoja> gravados = new HashMap<>();

        @Override
        public Optional<EntregadorDaLoja> buscar(UUID loja, UUID usuarioId) {
            return Optional.ofNullable(gravados.get(loja + "/" + usuarioId));
        }

        @Override
        public List<EntregadorDaLoja> daLoja(UUID loja) {
            return gravados.values().stream().filter(e -> e.estabelecimentoId().equals(loja)).toList();
        }

        @Override
        public EntregadorDaLoja salvar(EntregadorDaLoja e) {
            gravados.put(e.estabelecimentoId() + "/" + e.usuarioId(), e);
            return e;
        }
    }

    /**
     * O merchant: responde por loja, com as permissões dadas; loja sem resposta = sem vínculo.
     * Quem pergunta é sempre {@link #eu} — o portador do token, no teste.
     */
    static final class Merchant implements AutorizacaoComercialPort {
        final Map<UUID, Set<PermissaoDoPedido>> vinculos = new HashMap<>();
        UUID eu = UUID.randomUUID();

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
                    : Optional.of(new ContextoDeAcesso(eu, loja, permissoes));
        }
    }
}
