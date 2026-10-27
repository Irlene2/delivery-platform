package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.exception.AcessoNegado;
import com.deliveryplatform.order.application.exception.PedidoNaoEncontrado;
import com.deliveryplatform.order.application.port.out.AutorizacaoComercialPort;
import com.deliveryplatform.order.application.port.out.ContextoDeAcesso;
import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.domain.model.Pedido;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Os dois passos que toda rota do painel repete, na ordem que importa: autoriza, e só então
 * busca. Invertido, um pedido de outra loja responderia 404 antes de saber se quem pergunta
 * tem acesso à loja da URL.
 */
@Component
public class Autorizador {

    private final AutorizacaoComercialPort autorizacao;
    private final PedidoRepositorio pedidos;

    public Autorizador(AutorizacaoComercialPort autorizacao, PedidoRepositorio pedidos) {
        this.autorizacao = autorizacao;
        this.pedidos = pedidos;
    }

    /** Sem contexto é 403; sem a permissão também — a mesma recusa. */
    public void exigir(UUID estabelecimentoId, PermissaoDoPedido permissao) {
        ContextoDeAcesso contexto = autorizacao.contexto(estabelecimentoId)
                .orElseThrow(AcessoNegado::new);
        if (!contexto.pode(permissao)) {
            throw new AcessoNegado();
        }
    }

    /**
     * Autoriza e devolve o pedido <b>desta</b> loja. O identificador da URL nunca é confiado
     * (invariante 9): pedido de outra loja é 404, o mesmo de não existir.
     */
    public Pedido pedidoDaLoja(UUID estabelecimentoId, UUID pedidoId, PermissaoDoPedido permissao) {
        exigir(estabelecimentoId, permissao);
        return pedidos.buscarPorId(pedidoId)
                .filter(p -> p.getEstabelecimentoId().equals(estabelecimentoId))
                .orElseThrow(PedidoNaoEncontrado::new);
    }
}
