package com.deliveryplatform.merchant.application.usecase;

import com.deliveryplatform.merchant.application.port.in.ConsultarMinhasLojas;
import com.deliveryplatform.merchant.application.port.in.LojaDoUsuario;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos;
import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos.NomeDaLoja;
import com.deliveryplatform.merchant.domain.model.Membro;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Duas consultas e um filtro — e o filtro é a razão de esta classe existir em
 * vez de uma linha no controlador.
 *
 * <h2>{@code filter(Membro::ativo)}, outra vez</h2>
 *
 * <p>O {@code MembroRepositorio} devolve vínculo em <b>qualquer</b> estado, e o
 * javadoc dele diz com todas as letras: <i>"Quem resolve acesso a partir daqui
 * e <b>não</b> passa por {@code Membro.pode(...)} precisa filtrar
 * {@code ativo()} na mão"</i>. Este caso de uso não chama {@code pode(...)},
 * porque não exige permissão nenhuma — ele responde <i>quais</i> permissões há.
 *
 * <p>Sem o filtro, alguém demitido de uma loja continuaria vendo a loja no
 * seletor, entraria nela, e só receberia 403 na primeira ação. É a mesma
 * armadilha que a G-B2 desarmou na rota do contexto, pelo mesmo caminho, e é
 * por isso que o teste do estado suspenso é o primeiro deste caso de uso.
 *
 * <h2>Duas consultas, e não um {@code JOIN}</h2>
 *
 * <p>O {@code Membro} tem o identificador da loja; o <b>nome</b> mora no
 * {@code Estabelecimento}. São dois agregados, e agregado se carrega inteiro —
 * um {@code JOIN} entre eles devolveria uma terceira coisa que não é nenhum dos
 * dois.
 *
 * <p>São duas consultas e não N+1: a segunda busca <b>todos</b> os
 * estabelecimentos de uma vez, pela lista de identificadores. Com uma pessoa em
 * três lojas, são duas idas ao banco, não quatro.
 *
 * <h2>Loja que sumiu não aparece</h2>
 *
 * <p>Se houver vínculo ativo para um estabelecimento que a segunda consulta não
 * devolve, a linha é <b>descartada</b>. Não deveria acontecer — não há remoção
 * de estabelecimento neste sistema —, e se acontecer, mostrar uma loja sem nome
 * é pior do que não mostrá-la: o front abriria um painel de uma loja que não
 * existe e todas as chamadas dele dariam 403.
 */
@Service
public class ConsultarMinhasLojasService implements ConsultarMinhasLojas {

    private final MembroRepositorio membros;
    private final NomesDeEstabelecimentos nomes;

    public ConsultarMinhasLojasService(MembroRepositorio membros, NomesDeEstabelecimentos nomes) {
        this.membros = membros;
        this.nomes = nomes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LojaDoUsuario> de(UUID usuarioId) {
        List<Membro> ativos = membros.buscarPorUsuario(usuarioId).stream()
                .filter(Membro::ativo)
                .toList();

        if (ativos.isEmpty()) {
            return List.of();
        }

        Set<UUID> lojas = ativos.stream()
                .map(Membro::getEstabelecimentoId)
                .collect(Collectors.toUnmodifiableSet());

        Map<UUID, String> porId = nomes.de(lojas).stream()
                .collect(Collectors.toMap(NomeDaLoja::estabelecimentoId, NomeDaLoja::nome));

        return ativos.stream()
                .filter(membro -> porId.containsKey(membro.getEstabelecimentoId()))
                .map(membro -> new LojaDoUsuario(
                        membro.getEstabelecimentoId(),
                        porId.get(membro.getEstabelecimentoId()),
                        membro.getPapel(),
                        List.copyOf(membro.getPermissoes())))
                // Ordem estável, por nome: sem ela o seletor de loja troca de
                // ordem entre recargas, e a pessoa clica na loja errada.
                .sorted(Comparator.comparing(LojaDoUsuario::nome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
