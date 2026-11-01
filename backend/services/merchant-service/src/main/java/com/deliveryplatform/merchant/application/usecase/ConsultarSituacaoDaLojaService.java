package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.exception.LojaNaoEncontrada;
import com.deliveryplatform.merchant.application.port.in.ConsultarSituacaoDaLoja;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Pergunta ao agregado e devolve. <b>Sem vínculo e sem permissão</b>: quem pergunta é o
 * cliente pelo link, sem conta, e o {@code order} na hora de aceitar o pedido.
 *
 * <p>A conta não mora aqui — horário, pausa e fuso são do {@link Estabelecimento}. A próxima
 * abertura só é procurada com a loja fechada: aberta, ela não tem serventia.
 */
@Service
public class ConsultarSituacaoDaLojaService implements ConsultarSituacaoDaLoja {

    private final EstabelecimentoRepositorio lojas;
    private final Clock relogio;

    public ConsultarSituacaoDaLojaService(EstabelecimentoRepositorio lojas, Clock relogio) {
        this.lojas = lojas;
        this.relogio = relogio;
    }

    @Override
    @Transactional(readOnly = true)
    public SituacaoDaLoja daLoja(UUID estabelecimentoId) {
        Estabelecimento loja = lojas.buscarPorId(estabelecimentoId)
                .orElseThrow(LojaNaoEncontrada::new);

        Instant agora = Instant.now(relogio);
        boolean aberta = loja.estaAberta(agora);
        Optional<Instant> proxima = aberta ? Optional.empty() : loja.proximaAberturaApos(agora);
        return new SituacaoDaLoja(loja.getIdentificacao().nome(), aberta, proxima);
    }
}
