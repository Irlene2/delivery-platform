package com.deliveryplatform.order.domain.model;

/**
 * Como o cliente paga — sempre <b>na entrega ou no balcão</b> nesta entrega. A plataforma
 * não movimenta dinheiro; ela registra que o pagamento aconteceu (planejamento §1.1).
 */
public enum FormaDePagamento {
    DINHEIRO,
    CARTAO,
    PIX
}
