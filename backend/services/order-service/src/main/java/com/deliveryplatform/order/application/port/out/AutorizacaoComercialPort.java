package com.deliveryplatform.order.application.port.out;

import java.util.Optional;
import java.util.UUID;

/**
 * A mesma porta do {@code catalog} (ADR-011, emendada pela ADR-045): o token de quem pediu é
 * encaminhado ao {@code merchant}, e o caso de uso não o conhece.
 *
 * <p>Vazio = o {@code merchant} disse que não há vínculo ativo (403 lá). {@link
 * AutorizacaoIndisponivel} = ele não respondeu. As duas negam.
 */
public interface AutorizacaoComercialPort {

    Optional<ContextoDeAcesso> contexto(UUID estabelecimentoId);
}
