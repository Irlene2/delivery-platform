package com.deliveryplatform.merchant.application.port.in;

import java.util.UUID;

/**
 * A consulta que os outros serviços fazem para autorizar as requisições
 * <b>deles</b> — a peça que a ADR-011 desenhou e que nunca teve produtor.
 *
 * <p><b>Dois identificadores, e o segundo não vem de quem chama.</b> A ordem é a
 * mesma do {@link ConsultarEquipe}: o da URL primeiro, o do token depois. Mas a
 * diferença com o {@code ConsultarEquipe} é a razão de ser desta rodada — aqui
 * o {@code usuarioAutenticado} é o <b>sujeito da resposta</b>, e não só o
 * critério de acesso a ela.
 *
 * <p><b>Ela não exige permissão nenhuma do portador, e isso é decisão
 * escrita.</b> Pedir permissão para descobrir permissão é circular: qualquer
 * requisito aqui teria de ser resolvido por esta mesma consulta. O que torna a
 * ausência de requisito segura é a outra metade da decisão: <b>não existe
 * {@code usuarioId} na entrada</b> — nem no caminho, nem em parâmetro, nem em
 * cabeçalho. A rota responde sobre o portador do token e sobre mais ninguém, e
 * o portador já pode descobrir o próprio vínculo tentando usar a loja.
 *
 * <p><b>Lança {@code AcessoNegado}, não devolve vazio.</b> Do lado do consumidor
 * a ADR-011 desenha {@code Optional<ContextoDeAcesso>}; aqui, na borda HTTP, o
 * vazio é um <b>403</b> — e é o adaptador do consumidor que traduz 403 em
 * {@code Optional.empty()}. Quatro situações dão a mesma recusa: loja
 * inexistente, sem vínculo, vínculo suspenso e vínculo removido (M7).
 */
public interface ConsultarContextoDeAcesso {

    ContextoDeAcesso doPortador(UUID estabelecimentoId, UUID usuarioAutenticado);
}
