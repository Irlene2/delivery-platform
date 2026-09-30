package com.deliveryplatform.merchant.infrastructure.persistence.repository;

import com.deliveryplatform.merchant.infrastructure.persistence.entity.EstabelecimentoJpaEntity;
import org.springframework.data.repository.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Uma consulta só, e ela devolve <b>duas colunas</b>.
 *
 * <h2>Por que estende {@code Repository} e não {@code JpaRepository}</h2>
 *
 * <p>{@code org.springframework.data.repository.Repository} é a interface
 * marcadora vazia: esta interface expõe <b>exatamente</b> o método declarado
 * abaixo, e mais nada. Estendendo {@code JpaRepository} eu ganharia
 * {@code deleteAll}, {@code saveAll} e mais duas dúzias de métodos sobre um
 * agregado que esta camada não tem o direito de gravar — e um dia alguém os
 * usaria, porque estavam ali.
 *
 * <p>É o mesmo raciocínio do {@code equipeParaAlteracao} não ser usado para
 * exibir lista: a superfície estreita é a documentação.
 *
 * <h2>Projeção de interface, e não {@code select new}</h2>
 *
 * <p>Uma expressão de construtor em JPQL exigiria escrever o nome
 * completamente qualificado de um {@code record} <b>aninhado</b> dentro de uma
 * interface — e a forma desse nome (ponto ou cifrão) varia entre versões do
 * Hibernate. A projeção de interface não tem JPQL nenhum: o nome do método é a
 * consulta, e o Spring Data faz o {@code select} das duas colunas.
 *
 * <p>A conversão para {@code NomeDaLoja} acontece no adaptador, em Java, onde
 * um erro é erro de compilação.
 *
 * <h2>Sobre a entidade, e não sobre o agregado</h2>
 *
 * <p>O tipo é o {@code EstabelecimentoJpaEntity}, e não o {@code Estabelecimento}
 * do domínio: o agregado não tem anotação de persistência (ArchUnit) e guarda o
 * nome dentro da {@code Identificacao}. A entidade tem a chave {@code id} e o
 * nome como coluna {@code nome}, direto — conferido na G-B5, que é o que faz
 * {@code findAllByIdIn} com a projeção {@code getId()}/{@code getNome()}
 * funcionar sem caminho aninhado. As cinco coleções {@code EAGER} da entidade
 * não são carregadas: projeção de interface fechada seleciona só as duas
 * colunas.
 */
interface ConsultaDeNomesDeLojas extends Repository<EstabelecimentoJpaEntity, UUID> {

    List<Nome> findAllByIdIn(Collection<UUID> ids);

    /** As duas colunas, e só elas. */
    interface Nome {

        UUID getId();

        String getNome();
    }
}
