package com.deliveryplatform.order.application.usecase;

import com.deliveryplatform.order.application.port.in.ConfigurarPedidos;
import com.deliveryplatform.order.application.port.in.ConsultarConfiguracaoPublica;
import com.deliveryplatform.order.application.port.out.ConfiguracaoDePedidosRepositorio;
import com.deliveryplatform.order.application.port.out.PermissaoDoPedido;
import com.deliveryplatform.order.application.port.out.SituacaoDaLojaPort;
import com.deliveryplatform.order.application.port.out.SituacaoIndisponivel;
import com.deliveryplatform.order.domain.model.ConfiguracaoDePedidos;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * A taxa fixa por loja e as modalidades (decisão de 26/10/2026). Mora no {@code order} para a
 * entrega de 15/11 não mexer no agregado da loja no {@code merchant}.
 */
@Service
public class ConfiguracaoDePedidosService implements ConfigurarPedidos, ConsultarConfiguracaoPublica {

    private final Autorizador autorizador;
    private final ConfiguracaoDePedidosRepositorio configuracoes;
    private final SituacaoDaLojaPort situacoes;

    public ConfiguracaoDePedidosService(Autorizador autorizador,
                                        ConfiguracaoDePedidosRepositorio configuracoes,
                                        SituacaoDaLojaPort situacoes) {
        this.autorizador = autorizador;
        this.configuracoes = configuracoes;
        this.situacoes = situacoes;
    }

    /**
     * A ida ao {@code merchant} fica fora da transação: segurar conexão de banco durante uma
     * chamada HTTP é o que a ADR-011 manda não fazer.
     */
    @Override
    public Vitrine vitrine(UUID estabelecimentoId) {
        ConfiguracaoDePedidos configuracao = daLoja(estabelecimentoId);
        try {
            SituacaoDaLojaPort.SituacaoDaLoja s = situacoes.daLoja(estabelecimentoId);
            return new Vitrine(configuracao, s.aberta(), s.proximaAbertura().orElse(null));
        } catch (SituacaoIndisponivel semResposta) {
            return new Vitrine(configuracao, null, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoDePedidos daLoja(UUID estabelecimentoId) {
        return atual(estabelecimentoId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConfiguracaoDePedidos ler(UUID estabelecimentoId) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.CONFIGURAR_LOJA);
        return atual(estabelecimentoId);
    }

    @Override
    @Transactional
    public ConfiguracaoDePedidos definir(UUID estabelecimentoId, Money taxaDeEntrega,
                                         boolean aceitaEntrega, boolean aceitaRetirada) {
        autorizador.exigir(estabelecimentoId, PermissaoDoPedido.CONFIGURAR_LOJA);
        ConfiguracaoDePedidos configuracao = atual(estabelecimentoId);
        configuracao.definir(taxaDeEntrega, aceitaEntrega, aceitaRetirada);
        return configuracoes.salvar(configuracao);
    }

    private ConfiguracaoDePedidos atual(UUID estabelecimentoId) {
        return configuracoes.daLoja(estabelecimentoId)
                .orElseGet(() -> ConfiguracaoDePedidos.padrao(estabelecimentoId));
    }
}
