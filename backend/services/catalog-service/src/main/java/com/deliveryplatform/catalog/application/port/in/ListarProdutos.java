package com.deliveryplatform.catalog.application.port.in;

import com.deliveryplatform.catalog.domain.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * A primeira consulta autorizada do {@code catalog}.
 *
 * <p><b>Recebe o identificador da URL e a paginação, e não recebe o usuário.</b>
 * É a diferença para o {@code ConsultarEquipe} do {@code merchant}, que recebe
 * os dois: lá o serviço é dono do vínculo e o resolve sozinho; aqui quem
 * resolve é o {@code merchant}, e o portador sai do token dentro do adaptador
 * (ADR-038, ADR-045). O caso de uso não conhece o token, e isso é o título de
 * uma ADR, não um detalhe.
 *
 * <p><b>Paginação não é opcional.</b> O {@code CLAUDE.md} é curto a respeito:
 * <i>"Paginação obrigatória em toda listagem. Nada de {@code findAll()} sem
 * {@code Pageable}"</i>. A porta de saída do produto nasceu na G-B1 devolvendo
 * {@code List} — antes de existir qualquer listagem por HTTP —, e esta rodada
 * paga essa dívida.
 *
 * <p>{@code Pageable} e {@code Page} são do Spring Data, e estão nesta camada de
 * propósito: o ArchUnit protege o {@code domain}, não o {@code application}, e
 * todos os adaptadores desta porta são Spring Data de qualquer maneira.
 * Reimplementar {@code Pagina<T>} compraria pureza numa camada que a regra não
 * protege, ao preço de uma tradução em cada adaptador.
 */
public interface ListarProdutos {

    Page<Produto> publicadosDaLoja(UUID estabelecimentoId, Pageable paginacao);
}
