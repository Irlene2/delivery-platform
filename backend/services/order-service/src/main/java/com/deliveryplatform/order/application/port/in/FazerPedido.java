package com.deliveryplatform.order.application.port.in;

import com.deliveryplatform.order.domain.model.FormaDePagamento;
import com.deliveryplatform.order.domain.model.Modalidade;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.valuetypes.Money;

import java.util.List;
import java.util.UUID;

/**
 * O cliente fecha o carrinho pelo link do cardápio (ADR-077). Rota pública: não há usuário,
 * e o que identifica o pedido depois é o código de acompanhamento.
 */
public interface FazerPedido {

    Pedido fazer(UUID estabelecimentoId, Solicitacao solicitacao);

    /** O que veio da tela. Nenhum preço: o preço é lido no cardápio. */
    record Solicitacao(
            String nomeDoCliente,
            String telefone,
            Modalidade modalidade,
            EnderecoInformado endereco,
            List<ItemSolicitado> itens,
            FormaDePagamento formaDePagamento,
            Money trocoPara) {
    }

    record EnderecoInformado(String logradouro, String bairro, String complemento) {
    }

    record ItemSolicitado(UUID produtoId, int quantidade, String observacao) {
    }
}
