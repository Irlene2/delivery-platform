package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.ProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.ModoDeControle;
import com.deliveryplatform.catalog.domain.model.Produto;
import com.deliveryplatform.catalog.support.Infraestrutura;
import com.deliveryplatform.catalog.support.Precos;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O teste que provaria que a G-B1 estava escrevendo no lugar errado.
 *
 * <h2>O que aconteceu</h2>
 *
 * <p>Na G-B1 a classe-base apontava {@code spring.data.mongodb.uri}, chave que
 * o Spring Boot 4 depreciou com nível <b>error</b> — ela não é lida. O
 * contêiner do Testcontainers subiu, ficou de pé <b>sem ninguém falar com
 * ele</b>, e o contexto caiu no padrão {@code mongodb://localhost/test}, que na
 * máquina de desenvolvimento é o MongoDB do compose.
 *
 * <p>Os testes passaram. O {@code changeUnit} rodou. Os dois índices existiam.
 * E nada disso estava acontecendo onde se pensava que estava — inclusive a
 * resposta da pergunta que decidia a rodada inteira, que ficou certa por sorte.
 *
 * <h2>Por que a prova não compara endereços</h2>
 *
 * <p>O caminho óbvio seria afirmar que o cliente fala com
 * {@code MONGO.getMappedPort(27017)}. <b>Não funciona.</b> Num replica set, a
 * descrição do servidor traz o endereço <i>canônico</i> que o próprio nó
 * anuncia — no contêiner, o nome interno dele, algo como
 * {@code 4c450aa1e11a:27017} —, e não o {@code 127.0.0.1:43370} pelo qual o
 * cliente chegou. Um teste que comparasse os dois falharia estando tudo certo,
 * que é o pior tipo de teste: o que ensina a desligá-lo.
 *
 * <p><b>A prova é por efeito.</b> O serviço grava pela porta dele; o teste abre
 * uma conexão nova, construída a partir do <i>contêiner</i>, e procura o que foi
 * gravado. Se o serviço estivesse falando com outro servidor, o documento não
 * estaria aqui. Não depende de representação de endereço, de topologia nem de
 * ordem de descoberta.
 *
 * <p>O cliente direto precisa da representação de UUID declarada, pelo mesmo
 * motivo que a aplicação precisa: o padrão do driver é {@code unspecified}, e
 * sem isso ele recusa <i>ler</i> o {@code _id}.
 */
@SpringBootTest
class ConteinerDeVerdadeIT extends Infraestrutura {

    @Autowired ProdutoRepositorio produtos;
    @Autowired MongoDatabaseFactory fabrica;

    @Test
    @DisplayName("o banco é o catalog_db, e não o 'test' do padrão do driver")
    void o_banco_e_o_que_a_classe_base_pediu() {
        assertThat(fabrica.getMongoDatabase().getName())
                .as("'test' aqui significa que a URI da classe-base foi ignorada e o "
                        + "contexto caiu no padrão do driver — foi o que aconteceu na G-B1")
                .isEqualTo("catalog_db");
    }

    @Test
    @DisplayName("o que o serviço grava está DENTRO do contêiner deste teste")
    void o_servico_grava_no_conteiner() {
        Produto gravado = produtos.salvar(Produto.rascunho(
                UUID.randomUUID(), UUID.randomUUID(), "Coxinha",
                Precos.reais("8.00"), ModoDeControle.SEM_CONTROLE, 0));

        try (MongoClient direto = clienteDoConteiner()) {
            long achados = direto.getDatabase("catalog_db")
                    .getCollection("produtos")
                    .countDocuments(new Document("_id", gravado.getId()));

            assertThat(achados)
                    .as("o serviço gravou em algum lugar; se não foi aqui, foi no MongoDB "
                            + "de desenvolvimento de quem rodou o teste")
                    .isEqualTo(1);
        }
    }

    /**
     * Um cliente construído a partir do contêiner, e não da configuração da
     * aplicação. É a independência que faz a prova valer.
     */
    private static MongoClient clienteDoConteiner() {
        return MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(MONGO.getReplicaSetUrl("catalog_db")))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .build());
    }
}
