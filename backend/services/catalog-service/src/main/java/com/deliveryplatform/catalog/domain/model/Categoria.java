package com.deliveryplatform.catalog.domain.model;

import com.deliveryplatform.catalog.domain.exception.RegraDoCatalogoViolada;
import java.util.UUID;

/**
 * A seção do cardápio: "Pizzas salgadas", "Bebidas", "Sobremesas".
 *
 * <h2>Por que é raiz própria e não um campo do produto</h2>
 *
 * <p>Porque ela tem vida sem produto e produto sem ela seria pior. A categoria
 * é renomeada, reordenada e desativada <b>inteira</b> — "Sorvetes" some do
 * cardápio no inverno com um clique, e volta com outro. Se fosse uma string
 * dentro de cada produto, desativar a seção significaria varrer os produtos, e
 * renomear significaria uma migração que erra em qualquer produto criado no
 * meio do caminho.
 *
 * <p>O {@code catalogo.md} §1 já a lista como raiz, com quatro campos e nada
 * mais. Ela nasceu na G-A — e não na rodada seguinte — porque o {@link Produto}
 * carrega {@code categoriaId} e o segundo índice da §7 é
 * {@code (estabelecimentoId, categoriaId, ordem)}: um identificador que aponta
 * para um tipo que não existe é a mesma promessa com sintaxe de código que este
 * repositório recusou no enum sem produtor.
 *
 * <p><b>Ganhou persistência na E-3b</b> (ADR-064): porta, documento, coleção e
 * índice, e duas rotas — criar e listar. {@link #renomear}, {@link #reordenar},
 * {@link #ativar} e {@link #desativar} continuam sem rota, cada um com gatilho no
 * {@code dividas.md}; o {@code reordenar} arrasta o outbox do {@code catalog}, que
 * não existe.
 *
 * <h2>A {@code ordem} não tem regra, e é de propósito</h2>
 *
 * <p>O agregado aceita qualquer inteiro, inclusive repetido e negativo: o
 * {@code catalogo.md} §1 não escreve regra sobre ela, e a listagem desempata por
 * nome e id. Quem cria a seção escolhe onde ela fica.
 *
 * <h2>Categoria inativa não torna o produto invendável</h2>
 *
 * <p>A fórmula da §5 não menciona categoria, e o código segue a fórmula. Um
 * produto {@code ATIVO} numa categoria desativada continua {@code vendavel()}.
 * O efeito de desativar a categoria é de <b>montagem do cardápio</b> — a seção
 * não aparece, e com ela os produtos —, não de vendabilidade do item: quem
 * chegar ao produto por link direto ou por um pedido em conversa compra.
 *
 * <p>Se isso estiver errado, quem muda é a §5, e aí é uma linha aqui.
 */
public final class Categoria {

    private final UUID id;
    private final UUID estabelecimentoId;

    private String nome;
    private int ordem;
    private boolean ativa;

    /** A versão do documento (ADR-052), opaca: atravessa o agregado sem ser lida. */
    private Long versao;

    private Categoria(UUID id, UUID estabelecimentoId, String nome, int ordem, boolean ativa) {
        if (estabelecimentoId == null) {
            throw RegraDoCatalogoViolada.integridade("categoria sem estabelecimento");
        }
        this.id = id;
        this.estabelecimentoId = estabelecimentoId;
        this.nome = Textos.exigirPreenchido(nome, "nome da categoria");
        this.ordem = ordem;
        this.ativa = ativa;
    }

    /** Categoria nova nasce ativa: quem cria uma seção quer mostrá-la. */
    public static Categoria nova(UUID estabelecimentoId, String nome, int ordem) {
        return new Categoria(UUID.randomUUID(), estabelecimentoId, nome, ordem, true);
    }

    /**
     * O caminho de volta do banco. Passa pelas mesmas recusas da criação: um
     * documento sem loja ou sem nome não vira categoria.
     */
    public static Categoria reconstituir(UUID id, UUID estabelecimentoId, String nome,
                                         int ordem, boolean ativa, Long versao) {
        if (id == null) {
            throw RegraDoCatalogoViolada.integridade("categoria sem id");
        }
        Categoria categoria = new Categoria(id, estabelecimentoId, nome, ordem, ativa);
        categoria.versao = versao;
        return categoria;
    }

    public void renomear(String nome) {
        this.nome = Textos.exigirPreenchido(nome, "nome da categoria");
    }

    public void reordenar(int ordem) {
        this.ordem = ordem;
    }

    public void ativar() {
        this.ativa = true;
    }

    public void desativar() {
        this.ativa = false;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEstabelecimentoId() {
        return estabelecimentoId;
    }

    public String getNome() {
        return nome;
    }

    public int getOrdem() {
        return ordem;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public Long getVersao() {
        return versao;
    }
}
