package com.deliveryplatform.catalog.integration;

import com.deliveryplatform.catalog.application.port.out.CategoriaRepositorio;
import com.deliveryplatform.catalog.domain.model.Categoria;
import com.deliveryplatform.catalog.infrastructure.persistence.entity.CategoriaDocumento;
import com.deliveryplatform.catalog.support.Infraestrutura;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O adaptador da categoria contra o Mongo de verdade (ADR-064).
 */
@SpringBootTest
class CategoriaRepositorioIT extends Infraestrutura {

    @Autowired CategoriaRepositorio categorias;
    @Autowired MongoTemplate mongo;

    @BeforeEach
    void colecaoLimpa() {
        mongo.getCollection("categorias").deleteMany(new Document());
    }

    @Test
    @DisplayName("o que foi gravado volta igual: id, loja, nome, ordem e ativa")
    void ida_e_volta() {
        UUID loja = UUID.randomUUID();
        Categoria nova = Categoria.nova(loja, "Pizzas salgadas", 3);
        nova.desativar();

        categorias.salvar(nova);
        List<Categoria> lidas = categorias.daLoja(loja);

        assertThat(lidas).hasSize(1);
        Categoria lida = lidas.getFirst();
        assertThat(lida.getId()).isEqualTo(nova.getId());
        assertThat(lida.getEstabelecimentoId()).isEqualTo(loja);
        assertThat(lida.getNome()).isEqualTo("Pizzas salgadas");
        assertThat(lida.getOrdem()).isEqualTo(3);
        assertThat(lida.isAtiva())
                .as("gravada desativada de propósito: um mapeador que sempre escrevesse true passaria com uma nova")
                .isFalse();
    }

    /**
     * O {@code @Version} pelo mesmo critério do produto (ADR-052): uma gravação com a
     * versão velha é recusada. Hoje nenhuma rota regrava categoria; o teste prova que a
     * guarda já está lá quando renomear ganhar rota.
     */
    @Test
    @DisplayName("@Version: a gravação com versão velha é recusada")
    void a_versao_velha_e_recusada() {
        UUID loja = UUID.randomUUID();
        Categoria gravada = categorias.salvar(Categoria.nova(loja, "Bebidas", 0));
        assertThat(gravada.getVersao()).as("depois da inserção a versão existe").isNotNull();

        Categoria outraCopia = categorias.daLoja(loja).getFirst();
        gravada.renomear("Bebidas geladas");
        categorias.salvar(gravada);

        outraCopia.renomear("Refrigerantes");
        assertThatThrownBy(() -> categorias.salvar(outraCopia))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(mongo.findById(gravada.getId(), CategoriaDocumento.class).nome())
                .isEqualTo("Bebidas geladas");
    }
}
