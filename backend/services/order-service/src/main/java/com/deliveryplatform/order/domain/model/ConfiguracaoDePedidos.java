package com.deliveryplatform.order.domain.model;

import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada;
import com.deliveryplatform.order.domain.exception.RegraDoPedidoViolada.Motivo;
import com.deliveryplatform.valuetypes.Money;

import java.util.Objects;
import java.util.UUID;

/**
 * O que a loja aceita receber, nesta entrega: a taxa de entrega fixa e as modalidades.
 *
 * <p>Mora no {@code order}, e não no {@code merchant}, para a entrega de 15/11 não mexer
 * no agregado da loja (ADR-077). Quando a área por bairro existir (ADR-020), a taxa sai
 * daqui.
 */
public final class ConfiguracaoDePedidos {

    private final UUID estabelecimentoId;
    private Money taxaDeEntrega;
    private boolean aceitaEntrega;
    private boolean aceitaRetirada;

    private ConfiguracaoDePedidos(UUID estabelecimentoId, Money taxaDeEntrega,
                                  boolean aceitaEntrega, boolean aceitaRetirada) {
        this.estabelecimentoId = Objects.requireNonNull(estabelecimentoId, "estabelecimentoId");
        definir(taxaDeEntrega, aceitaEntrega, aceitaRetirada);
    }

    /** A loja que nunca configurou: taxa zero, as duas modalidades. */
    public static ConfiguracaoDePedidos padrao(UUID estabelecimentoId) {
        return new ConfiguracaoDePedidos(estabelecimentoId, Money.ZERO, true, true);
    }

    public static ConfiguracaoDePedidos reconstituir(UUID estabelecimentoId, Money taxaDeEntrega,
                                                     boolean aceitaEntrega, boolean aceitaRetirada) {
        return new ConfiguracaoDePedidos(estabelecimentoId, taxaDeEntrega, aceitaEntrega, aceitaRetirada);
    }

    public void definir(Money taxa, boolean entrega, boolean retirada) {
        if (taxa == null) {
            throw RegraDoPedidoViolada.integridade("taxa de entrega ausente");
        }
        if (taxa.ehNegativo()) {
            throw new RegraDoPedidoViolada(Motivo.TAXA_NEGATIVA, "taxa " + taxa);
        }
        if (!entrega && !retirada) {
            throw new RegraDoPedidoViolada(Motivo.NENHUMA_MODALIDADE, "nenhuma modalidade aceita");
        }
        this.taxaDeEntrega = taxa;
        this.aceitaEntrega = entrega;
        this.aceitaRetirada = retirada;
    }

    public boolean aceita(Modalidade modalidade) {
        return modalidade == Modalidade.ENTREGA ? aceitaEntrega : aceitaRetirada;
    }

    /** Na retirada, a taxa é zero — e não nula (pedido.md §1). */
    public Money taxaPara(Modalidade modalidade) {
        return modalidade == Modalidade.ENTREGA ? taxaDeEntrega : Money.ZERO;
    }

    public UUID getEstabelecimentoId() {
        return estabelecimentoId;
    }

    public Money getTaxaDeEntrega() {
        return taxaDeEntrega;
    }

    public boolean isAceitaEntrega() {
        return aceitaEntrega;
    }

    public boolean isAceitaRetirada() {
        return aceitaRetirada;
    }
}
