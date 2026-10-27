package com.deliveryplatform.order.infrastructure.persistence.mapper;

import com.deliveryplatform.order.domain.model.Cliente;
import com.deliveryplatform.order.domain.model.Endereco;
import com.deliveryplatform.order.domain.model.ItemDoPedido;
import com.deliveryplatform.order.domain.model.PagamentoDeclarado;
import com.deliveryplatform.order.domain.model.PagamentoRegistrado;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.Telefone;
import com.deliveryplatform.order.infrastructure.persistence.entity.ItemDoPedidoJpa;
import com.deliveryplatform.order.infrastructure.persistence.entity.PedidoJpaEntity;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PedidoJpaMapper {

    public PedidoJpaEntity paraEntidade(Pedido p) {
        Endereco e = p.getEndereco();
        PagamentoRegistrado r = p.getPagamentoRegistrado();
        return new PedidoJpaEntity(
                p.getId(),
                p.getEstabelecimentoId(),
                p.getNumero(),
                p.getCodigoDeAcompanhamento(),
                p.getCliente().nome(),
                p.getCliente().telefone().numero(),
                p.getModalidade(),
                e == null ? null : e.logradouro(),
                e == null ? null : e.bairro(),
                e == null ? null : e.complemento(),
                p.getSubtotal().valor(),
                p.getTaxaDeEntrega().valor(),
                p.getTotal().valor(),
                p.getPagamentoDeclarado().forma(),
                valor(p.getPagamentoDeclarado().trocoPara()),
                p.getStatus(),
                p.getMotivoDoCancelamento(),
                p.getEntregador(),
                r == null ? null : r.formaEfetiva(),
                r == null ? null : valor(r.valorRecebido()),
                r == null ? null : valor(r.trocoDevolvido()),
                r == null ? null : r.pixConferido(),
                r == null ? null : r.registradoEm(),
                p.getCriadoEm(),
                p.getAtualizadoEm(),
                p.getItens().stream()
                        .map(i -> new ItemDoPedidoJpa(i.produtoId(), i.nome(),
                                i.precoUnitario().valor(), i.quantidade(), i.observacao()))
                        .toList(),
                p.getVersao());
    }

    public Pedido paraDominio(PedidoJpaEntity e) {
        Endereco endereco = e.getEnderecoLogradouro() == null ? null
                : new Endereco(e.getEnderecoLogradouro(), e.getEnderecoBairro(),
                        e.getEnderecoComplemento());
        PagamentoRegistrado registrado = e.getFormaEfetiva() == null ? null
                : new PagamentoRegistrado(e.getFormaEfetiva(), money(e.getValorRecebido()),
                        money(e.getTrocoDevolvido()), Boolean.TRUE.equals(e.getPixConferido()),
                        e.getPagamentoRegistradoEm());
        return Pedido.reconstituir(
                e.getId(),
                e.getEstabelecimentoId(),
                e.getNumero(),
                e.getCodigoDeAcompanhamento(),
                new Cliente(e.getClienteNome(), Telefone.de(e.getClienteTelefone())),
                e.getModalidade(),
                endereco,
                e.getItens().stream()
                        .map(i -> new ItemDoPedido(i.getProdutoId(), i.getNome(),
                                Money.de(i.getPrecoUnitario()), i.getQuantidade(),
                                i.getObservacao()))
                        .toList(),
                Money.de(e.getSubtotal()),
                Money.de(e.getTaxaDeEntrega()),
                Money.de(e.getTotal()),
                new PagamentoDeclarado(e.getFormaDeclarada(), money(e.getTrocoPara())),
                e.getCriadoEm(),
                e.getStatus(),
                e.getMotivoDoCancelamento(),
                e.getEntregador(),
                registrado,
                e.getAtualizadoEm(),
                e.getVersao());
    }

    private static BigDecimal valor(Money dinheiro) {
        return dinheiro == null ? null : dinheiro.valor();
    }

    private static Money money(BigDecimal valor) {
        return valor == null ? null : Money.de(valor);
    }
}
