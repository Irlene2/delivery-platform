package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.support.Infraestrutura;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <b>O teste que decide a rodada.</b>
 *
 * <p>Ele responde uma pergunta que o repositório carregava havia quatro meses
 * sem ninguém fazer: <b>o Mongock roda?</b> Ele está no classpath do
 * {@code catalog} desde o primeiro commit, o {@code application.yml} aponta o
 * pacote de varredura, e nenhum {@code changeUnit} jamais executou — porque
 * faltava {@code @EnableMongock} e porque nenhum teste deste serviço subia
 * contexto Spring.
 *
 * <p>Há um risco de fato externo em cima disso: o Mongock 5.5.1 é de março de
 * 2025 e traz o driver {@code mongodb-springdata-v4-driver}, enquanto o Spring
 * Boot 4.1.1 traz Spring Data MongoDB <b>5.1.1</b>. O nome do artefato diz
 * "v4". Se a incompatibilidade for real, ela aparece aqui — no startup, e não
 * em produção.
 *
 * <p>Este teste afirma o <b>resultado</b>, não o mecanismo: os dois índices
 * existem. Um Mongock que suba, registre o runner e não execute nada faria o
 * contexto passar e este teste falhar, que é exatamente a ordem certa.
 */
@SpringBootTest
class MongockIT extends Infraestrutura {

    @Autowired MongoTemplate mongo;

    private List<Document> indicesDeProdutos() {
        return StreamSupport
                .stream(mongo.getCollection("produtos").listIndexes().spliterator(), false)
                .toList();
    }

    @Test
    @DisplayName("C13: o changeUnit rodou e os dois índices da §7 existem")
    void c13_os_indices_existem() {
        assertThat(indicesDeProdutos())
                .as("se vier só o _id_, o changeUnit não executou — e o serviço teria "
                        + "subido em silêncio achando que migrou")
                .extracting(indice -> indice.getString("name"))
                .contains("produto_por_estabelecimento_e_estado",
                        "produto_por_estabelecimento_categoria_e_ordem");
    }

    @Test
    @DisplayName("as chaves dos índices são as da §7, na ordem escrita")
    void as_chaves_sao_as_da_secao_7() {
        Document porEstado = indiceChamado("produto_por_estabelecimento_e_estado");
        Document porCategoria = indiceChamado("produto_por_estabelecimento_categoria_e_ordem");

        assertThat(porEstado.get("key", Document.class).keySet())
                .as("a ordem das chaves de um índice composto não é decorativa: um índice "
                        + "que não comece pelo estabelecimentoId obriga a varrer documentos "
                        + "de todas as lojas para responder sobre uma")
                .containsExactly("estabelecimentoId", "estadoDePublicacao");

        assertThat(porCategoria.get("key", Document.class).keySet())
                .containsExactly("estabelecimentoId", "categoriaId", "ordem");
    }

    /**
     * O índice da categoria, lido do banco (ADR-064 §5). A C13 é sobre o que o Mongock
     * criou, e não sobre o {@code changeUnit} existir no código.
     */
    @Test
    @DisplayName("C13: o índice da categoria existe no banco, com as chaves da §7")
    void c13_o_indice_da_categoria_existe() {
        Document porOrdem = StreamSupport
                .stream(mongo.getCollection("categorias").listIndexes().spliterator(), false)
                .filter(i -> "categoria_por_estabelecimento_e_ordem".equals(i.getString("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "o changeUnit 002 não criou o índice da categoria"));

        assertThat(porOrdem.get("key", Document.class).keySet())
                .containsExactly("estabelecimentoId", "ordem");
    }

    /**
     * O Mongock registra cada {@code changeUnit} executado numa coleção própria, e é
     * ela que faz uma unidade rodar uma vez só. Os dois {@code changeUnit} do
     * {@code catalog} estão lá, uma vez cada, com sucesso.
     */
    @Test
    @DisplayName("cada changeUnit está registrado uma vez, executado")
    void cada_change_unit_rodou_uma_vez() {
        List<Document> registros = StreamSupport
                .stream(mongo.getCollection("mongockChangeLog").find().spliterator(), false)
                .filter(r -> "EXECUTED".equals(r.getString("state")))
                .filter(r -> "EXECUTION".equals(r.getString("type")))
                .toList();

        assertThat(registros)
                .extracting(r -> r.getString("changeId"))
                .containsOnlyOnce("001-cria-indices-do-produto", "002-cria-indice-da-categoria");
    }

    private Document indiceChamado(String nome) {
        return indicesDeProdutos().stream()
                .filter(i -> nome.equals(i.getString("name")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("índice não existe: " + nome));
    }
}
