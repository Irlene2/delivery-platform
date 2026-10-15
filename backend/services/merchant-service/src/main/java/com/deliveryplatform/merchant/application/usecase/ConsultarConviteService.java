package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.port.in.ConsultarConvite;
import com.deliveryplatform.merchant.application.port.out.ConviteRepositorio;
import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos;
import com.deliveryplatform.merchant.domain.exception.ConviteInvalido;
import com.deliveryplatform.merchant.domain.model.Convite;
import com.deliveryplatform.merchant.domain.model.Papel;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * A leitura pública do convite (ADR-069 §2). <b>Sem autorização nenhuma</b>: o token é o
 * endereço e a credencial ao mesmo tempo, e quem não o tem não chega aqui.
 *
 * <p><b>Inexistente, expirado, aceito e cancelado são a mesma recusa</b> — a
 * {@link ConviteInvalido}, que o próprio domínio já usa para os quatro motivos (ADR-069 §3).
 *
 * <p><b>O papel é sempre {@code COLABORADOR}</b>: o convite não carrega papel, porque o
 * aceite nunca concede outro (B2). O que se oferece são permissões, e elas não saem — a
 * página diz o mínimo que convence.
 */
@Service
public class ConsultarConviteService implements ConsultarConvite {

    private final ConviteRepositorio convites;
    private final NomesDeEstabelecimentos nomes;
    private final Clock relogio;

    public ConsultarConviteService(ConviteRepositorio convites, NomesDeEstabelecimentos nomes,
                                   Clock relogio) {
        this.convites = convites;
        this.nomes = nomes;
        this.relogio = relogio;
    }

    @Override
    public ConviteParaQuemRecebe porToken(String token) {
        Instant agora = Instant.now(relogio);
        Convite convite = convites.buscarPorToken(token)
                .filter(achado -> achado.pendente(agora))
                .orElseThrow(ConviteInvalido::new);

        String nome = nomes.de(List.of(convite.getEstabelecimentoId())).stream()
                .findFirst()
                .map(NomesDeEstabelecimentos.NomeDaLoja::nome)
                .orElseThrow(ConviteInvalido::new);

        return new ConviteParaQuemRecebe(nome, Papel.COLABORADOR, convite.getExpiraEm());
    }
}
