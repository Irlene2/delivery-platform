package com.deliveryplatform.catalog.infrastructure.semeadura;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.config.SemeaduraProperties;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.domain.model.Disponibilidade;
import com.deliveryplatform.catalog.domain.model.GrupoDeOpcoes;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Opcao;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.valuetypes.Money;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * O produto da fixture de desenvolvimento — ADR-059. <b>Andaime, não
 * fundação</b>.
 *
 * <p>Andaime: a condição da saída está na ADR-059, emenda de 16/10/2026 — a lista do que
 * esta fixture grava e a API não escreve.
 *
 * <p>Atrás de {@code delivery.semeadura.ligada}, falsa por padrão. Grava
 * <b>uma</b> categoria e <b>um</b> produto nela (e, desde 01/11/2026, o cardápio de
 * demonstração — abaixo), na loja da {@link Fixture}. A
 * categoria é gravada desde a E-3b (ADR-064); até lá o produto apontava para um id
 * constante sem linha nenhuma, "solto de propósito". O produto: publicado, {@code ESGOTADO_HOJE},
 * e com uma opção do grupo também {@code ESGOTADO_HOJE} — a reativação trata
 * produto e opção, e assim a prova cobre os dois caminhos.
 *
 * <h2>Pelo agregado, e é por isso que o id é sorteado</h2>
 *
 * {@code rascunho} → {@code acrescentarGrupo} → {@code publicar} → {@code marcar}:
 * o mesmo caminho que um comerciante faria, com C1, C4 e C5 cobradas na
 * publicação. A {@code reconstituir} daria um id fixo, mas pula as regras de
 * publicação — é leitura, não escrita. Então a idempotência é pela <b>loja</b>:
 * se ela já tem produto publicado, não faz nada — nem a categoria, que nasce junto.
 *
 * <h2>Trinta dias atrás, e não ontem</h2>
 *
 * Calcular "ontem" exigiria o fuso da loja e a hora de corte das 04:00, e esse
 * cálculo tem um lugar só, o {@code merchant} (ADR-046 §6). Trinta dias atrás,
 * em UTC, é estritamente menor que qualquer expediente corrente em qualquer
 * fuso, e o predicado da reativação é {@code <}.
 *
 * <p><b>Não espera a loja abrir e não sabe de expediente.</b> Grava e termina.
 * Quem reativa é o consumidor, quando o evento chegar.
 *
 * <h2>O cardápio de demonstração — emenda de 01/11/2026</h2>
 *
 * Depois da margherita, grava o {@link CardapioDeDemonstracao}: as seis seções do protótipo,
 * 24 produtos publicados e disponíveis, com descrição e foto, e os grupos "Ponto da carne" e
 * "Adicionais" no {@value CardapioDeDemonstracao#COM_OPCOES}. A idempotência dele é por
 * <b>seção</b>, pelo nome: a seção que a loja já tem não é gravada de novo, nem os produtos
 * dela. É por isso que um banco antigo, que já tem a margherita, ganha o cardápio na próxima
 * subida, sem precisar apagar o volume.
 */
@Component
public class SemeaduraDoCatalogo implements ApplicationRunner {

    static final Duration IDADE_DO_CARIMBO = Duration.ofDays(30);

    /** Mais que o cardápio inteiro; a loja da fixture não passa disso. */
    static final int TETO_DA_LOJA = 200;

    private static final Logger log = LoggerFactory.getLogger(SemeaduraDoCatalogo.class);

    private final SemeaduraProperties propriedades;
    private final CategoriaRepositorio categorias;
    private final ProdutoRepositorio produtos;
    private final Clock relogio;

    public SemeaduraDoCatalogo(SemeaduraProperties propriedades,
                               CategoriaRepositorio categorias,
                               ProdutoRepositorio produtos,
                               Clock relogio) {
        this.propriedades = propriedades;
        this.categorias = categorias;
        this.produtos = produtos;
        this.relogio = relogio;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        if (!propriedades.ligada()) {
            return;
        }
        if (produtos.daLoja(Fixture.LOJA, PageRequest.of(0, 1)).hasContent()) {
            log.info("semeadura: a loja da fixture já tem produto — a margherita não é gravada");
        } else {
            semearAMargherita();
        }
        semearOCardapioDeDemonstracao();
        completarAsFotos();
    }

    private void semearAMargherita() {
        Instant marcadoEm = relogio.instant().minus(IDADE_DO_CARIMBO);
        LocalDate carimbo = LocalDate.ofInstant(marcadoEm, ZoneOffset.UTC);
        Disponibilidade esgotado = Disponibilidade.esgotadoHoje(marcadoEm, carimbo);

        Categoria pizzas = categorias.salvar(Categoria.nova(Fixture.LOJA, "Pizzas", 0));

        Produto produto = Produto.rascunho(Fixture.LOJA, pizzas.getId(), "Pizza margherita",
                Money.de("49.90"), ModoDeControle.QUALITATIVO, 0);
        produto.acrescentarGrupo(GrupoDeOpcoes.novo("Tamanho", 1, 1, 0, List.of(
                Opcao.nova("Pequena", Money.ZERO, 0),
                Opcao.nova("Média", Money.de("8.00"), 1),
                Opcao.nova("Grande", Money.de("16.00"), 2))));
        produto.publicar();
        produto.marcar(esgotado);
        GrupoDeOpcoes tamanho = produto.getGruposDeOpcoes().get(0);
        produto.marcarOpcao(tamanho.id(), tamanho.opcoes().get(2).id(), esgotado);

        Produto salvo = produtos.salvar(produto);
        log.info("semeadura: produto {} gravado na loja {}, ESGOTADO_HOJE com carimbo {}",
                salvo.getId(), Fixture.LOJA, carimbo);
    }

    private void semearOCardapioDeDemonstracao() {
        Set<String> jaTem = categorias.daLoja(Fixture.LOJA).stream()
                .map(Categoria::getNome)
                .collect(Collectors.toSet());
        // A margherita fica na seção 0; o cardápio vem depois dela.
        int ordemDaSecao = 1;
        int gravados = 0;
        for (CardapioDeDemonstracao.Secao secao : CardapioDeDemonstracao.SECOES) {
            int ordem = ordemDaSecao++;
            if (jaTem.contains(secao.nome())) {
                continue;
            }
            Categoria categoria = categorias.salvar(Categoria.nova(Fixture.LOJA, secao.nome(), ordem));
            int ordemDoItem = 0;
            for (CardapioDeDemonstracao.Item item : secao.itens()) {
                produtos.salvar(produtoDoCardapio(categoria, item, ordemDoItem++));
                gravados++;
            }
        }
        if (gravados > 0) {
            log.info("semeadura: {} produtos do cardápio de demonstração gravados na loja {}",
                    gravados, Fixture.LOJA);
        }
    }

    /**
     * O produto do cardápio de demonstração que está sem foto ganha a do cardápio. Cobre o banco
     * semeado antes de as quatro fotos da Commons entrarem (02/11/2026): a idempotência por seção
     * não regrava produto, então sem isto eles ficariam sem foto para sempre.
     */
    private void completarAsFotos() {
        Map<String, String> fotos = new HashMap<>();
        for (CardapioDeDemonstracao.Secao secao : CardapioDeDemonstracao.SECOES) {
            for (CardapioDeDemonstracao.Item item : secao.itens()) {
                if (item.imagem() != null) {
                    fotos.put(item.nome(), item.imagem());
                }
            }
        }
        int completados = 0;
        for (Produto produto : produtos.daLoja(Fixture.LOJA, PageRequest.of(0, TETO_DA_LOJA)).getContent()) {
            String foto = fotos.get(produto.getNome());
            if (foto != null && produto.getImagemRef() == null) {
                produto.ilustrarCom(foto);
                produtos.salvar(produto);
                completados++;
            }
        }
        if (completados > 0) {
            log.info("semeadura: {} produtos da loja {} ganharam a foto do cardápio", completados,
                    Fixture.LOJA);
        }
    }

    private static Produto produtoDoCardapio(Categoria categoria, CardapioDeDemonstracao.Item item,
                                             int ordem) {
        Produto produto = Produto.rascunho(Fixture.LOJA, categoria.getId(), item.nome(),
                Money.de(item.preco()), ModoDeControle.QUALITATIVO, ordem);
        produto.descreverCom(item.descricao());
        produto.ilustrarCom(item.imagem());
        if (item.nome().equals(CardapioDeDemonstracao.COM_OPCOES)) {
            produto.acrescentarGrupo(GrupoDeOpcoes.novo("Ponto da carne", 1, 1, 0, List.of(
                    Opcao.nova("Ao ponto", Money.ZERO, 0),
                    Opcao.nova("Bem passado", Money.ZERO, 1))));
            produto.acrescentarGrupo(GrupoDeOpcoes.novo("Adicionais", 0, 2, 1, List.of(
                    Opcao.nova("Bacon", Money.de("6.00"), 0),
                    Opcao.nova("Cheddar", Money.de("4.00"), 1))));
        }
        produto.publicar();
        return produto;
    }
}
