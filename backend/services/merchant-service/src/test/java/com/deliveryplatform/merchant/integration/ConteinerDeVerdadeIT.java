package com.deliveryplatform.merchant.integration;

import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.support.Infraestrutura;
import com.deliveryplatform.merchant.support.LojaDeTeste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O mesmo teste do {@code catalog}, e aqui ele importa mais.
 *
 * <p>Na G-B1 descobriu-se que uma classe-base de contêiner pode subir o
 * contêiner e o serviço falar com outro servidor — no caso do {@code catalog},
 * com o MongoDB de desenvolvimento da máquina, porque a chave da URI havia sido
 * depreciada no Boot 4 e o contexto caiu no padrão do driver.
 *
 * <p><b>Aqui a aposta é maior.</b> Vários ITs deste serviço fazem
 * {@code truncate table estabelecimento cascade} no {@code @BeforeEach} — a
 * lição da rodada F, sobre o banco compartilhado entre testes. Se este serviço
 * estivesse falando com o PostgreSQL de desenvolvimento, <b>os testes estariam
 * apagando dados reais a cada execução</b>, e em silêncio.
 *
 * <p>O {@code spring.datasource.url} não foi depreciado no Boot 4, então a
 * expectativa é que esteja tudo certo. <b>"A expectativa é que esteja tudo
 * certo" é exatamente o que este repositório não aceita</b> como resposta, e é
 * o que a G-B1 provou custar caro.
 *
 * <p>A prova é por efeito, e não por endereço: o serviço grava pela porta dele,
 * e o teste procura o registro numa conexão aberta a partir do <i>contêiner</i>.
 * Comparar strings de conexão provaria que a configuração está escrita; isto
 * prova onde o dado foi.
 */
@SpringBootTest
class ConteinerDeVerdadeIT extends Infraestrutura {

    @Autowired EstabelecimentoRepositorio lojas;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void limpar() {
        // Mesma razão da rodada F: o banco é compartilhado por toda a execução,
        // e um teste que não limpa vê as lojas dos outros.
        jdbc.execute("truncate table estabelecimento cascade");
    }

    @Test
    @DisplayName("o que o serviço grava está DENTRO do contêiner deste teste")
    void o_servico_grava_no_conteiner() throws Exception {
        UUID loja = lojas.salvar(LojaDeTeste.pizzaria()).getId();

        try (Connection direta = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             PreparedStatement consulta = direta.prepareStatement(
                     "select count(*) from estabelecimento where id = ?")) {

            consulta.setObject(1, loja);
            try (ResultSet linha = consulta.executeQuery()) {
                assertThat(linha.next()).isTrue();
                assertThat(linha.getLong(1))
                        .as("se este registro não estiver aqui, os truncate deste serviço "
                                + "estão caindo em outro banco — e apagando o que houver nele")
                        .isEqualTo(1L);
            }
        }
    }

    @Test
    @DisplayName("a conexão que a aplicação abriu é a do contêiner, e não a 5432 da máquina")
    void a_conexao_da_aplicacao_e_a_do_conteiner() throws Exception {
        String urlEfetiva;
        try (Connection daAplicacao = jdbc.getDataSource().getConnection()) {
            // getMetaData().getURL() é do JDBC, não do pool: não depende de o
            // Hikari ser o pool de hoje, e diz o endereço de quem está do outro
            // lado da conexão que a aplicação de fato abriu.
            urlEfetiva = daAplicacao.getMetaData().getURL();
        }

        assertThat(urlEfetiva)
                .as("o Testcontainers publica numa porta alta e aleatória (%s); um 5432 "
                        + "aqui significaria que a aplicação achou o PostgreSQL do compose",
                        POSTGRES.getJdbcUrl())
                .contains(":" + POSTGRES.getMappedPort(5432) + "/")
                .doesNotContain(":5432/");
    }
}
