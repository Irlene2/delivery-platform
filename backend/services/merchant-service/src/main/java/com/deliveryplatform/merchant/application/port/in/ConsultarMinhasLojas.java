package com.deliveryplatform.merchant.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * As lojas de que uma pessoa faz parte.
 *
 * <p><b>Recebe o {@code usuarioId}, e a diferença para a G-B2 é o chamador.</b>
 * O {@code ConsultarContextoDeAcesso} não recebe usuário nenhum porque quem o
 * chama é <i>outro serviço</i>, e a ADR-045 exige que ele prove o portador
 * encaminhando o token em vez de afirmá-lo. Aqui quem chama é o
 * <b>controlador deste serviço</b>, que acabou de extrair o {@code sub} do
 * token validado pela própria cadeia de filtros (ADR-038). Não há nada a
 * provar entre duas camadas do mesmo processo.
 *
 * <p>Devolve <b>vazio</b> para quem não tem vínculo ativo em loja nenhuma —
 * nunca exceção. Não ter loja é uma resposta legítima: é o estado de todo mundo
 * no instante seguinte ao cadastro, e a tela que a recebe é a que convida a
 * pessoa a pedir um convite.
 */
public interface ConsultarMinhasLojas {

    List<LojaDoUsuario> de(UUID usuarioId);
}
