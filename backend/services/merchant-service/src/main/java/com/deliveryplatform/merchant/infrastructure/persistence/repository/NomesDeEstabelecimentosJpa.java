package com.deliveryplatform.merchant.infrastructure.persistence.repository;

import com.deliveryplatform.merchant.application.port.out.NomesDeEstabelecimentos;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * A porta de projeção ligada ao banco — sete linhas, e duas delas são a decisão.
 *
 * <p><b>A guarda do vazio.</b> Uma coleção vazia vira {@code id in ()} em SQL,
 * que é sintaxe inválida em alguns bancos e consulta inútil em todos. O caso de
 * uso já não chama esta porta sem vínculo ativo — e há um teste que prova isso
 * —, mas a porta é pública e um segundo chamador aparecerá. A guarda mora dos
 * dois lados de propósito.
 *
 * <p><b>Devolve só o que achou.</b> Identificador que não existe não volta, e
 * é o contrato escrito no javadoc da porta. Quem chama é que decide o que fazer
 * com a diferença; aqui, nada.
 *
 * <p>Não tem {@code @Transactional}: quem abre a transação de leitura é o caso
 * de uso, e esta é uma consulta dentro dela.
 */
@Component
public class NomesDeEstabelecimentosJpa implements NomesDeEstabelecimentos {

    private final ConsultaDeNomesDeLojas consulta;

    public NomesDeEstabelecimentosJpa(ConsultaDeNomesDeLojas consulta) {
        this.consulta = consulta;
    }

    @Override
    public List<NomeDaLoja> de(Collection<UUID> estabelecimentoIds) {
        if (estabelecimentoIds.isEmpty()) {
            return List.of();
        }
        return consulta.findAllByIdIn(estabelecimentoIds).stream()
                .map(linha -> new NomeDaLoja(linha.getId(), linha.getNome()))
                .toList();
    }
}
