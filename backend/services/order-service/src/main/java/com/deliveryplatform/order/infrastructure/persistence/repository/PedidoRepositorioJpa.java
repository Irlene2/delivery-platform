package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.application.port.out.PedidoRepositorio;
import com.deliveryplatform.order.domain.model.Pedido;
import com.deliveryplatform.order.domain.model.StatusDoPedido;
import com.deliveryplatform.order.infrastructure.persistence.mapper.PedidoJpaMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PedidoRepositorioJpa implements PedidoRepositorio {

    /**
     * Um comando só, e atômico: cria a linha da loja com 1, ou soma 1 e trava a linha até o
     * fim da transação. O segundo pedido simultâneo da mesma loja espera aqui, e recebe o
     * número seguinte.
     */
    private static final String PROXIMO_NUMERO = """
            INSERT INTO numeracao_de_pedido (estabelecimento_id, ultimo) VALUES (?1, 1)
            ON CONFLICT (estabelecimento_id)
            DO UPDATE SET ultimo = numeracao_de_pedido.ultimo + 1
            RETURNING ultimo
            """;

    private final PedidoSpringDataRepository springData;
    private final PedidoJpaMapper mapper;

    /**
     * {@code EntityManager}, e não {@code JdbcTemplate}, pelo motivo que o
     * {@code AberturaDeExpedienteJdbc} do merchant escreveu: mesmo contexto, mesma transação,
     * sem depender de como o gerenciador expõe a conexão. O número e o pedido commitam juntos.
     */
    @PersistenceContext
    private EntityManager em;

    public PedidoRepositorioJpa(PedidoSpringDataRepository springData, PedidoJpaMapper mapper) {
        this.springData = springData;
        this.mapper = mapper;
    }

    @Override
    public Pedido salvar(Pedido pedido) {
        // saveAndFlush: o conflito de versão aparece aqui, dentro do caso de uso, e não no
        // commit — onde viraria uma exceção de transação sem o nome do problema.
        return mapper.paraDominio(springData.saveAndFlush(mapper.paraEntidade(pedido)));
    }

    @Override
    public Optional<Pedido> buscarPorId(UUID id) {
        return springData.findById(id).map(mapper::paraDominio);
    }

    @Override
    public Optional<Pedido> buscarPorCodigo(UUID codigoDeAcompanhamento) {
        return springData.findByCodigoDeAcompanhamento(codigoDeAcompanhamento)
                .map(mapper::paraDominio);
    }

    @Override
    public List<Pedido> daLoja(UUID estabelecimentoId, StatusDoPedido status, Instant desde,
                               Instant ate, int limite) {
        var teto = PageRequest.of(0, limite);
        var linhas = status == null
                ? springData.findByEstabelecimentoIdAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
                        estabelecimentoId, desde, ate, teto)
                : springData.findByEstabelecimentoIdAndStatusAndCriadoEmGreaterThanEqualAndCriadoEmLessThanOrderByCriadoEmDesc(
                        estabelecimentoId, status, desde, ate, teto);
        return linhas.stream().map(mapper::paraDominio).toList();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public long proximoNumero(UUID estabelecimentoId) {
        Object numero = em.createNativeQuery(PROXIMO_NUMERO)
                .setParameter(1, estabelecimentoId)
                .getSingleResult();
        return ((Number) numero).longValue();
    }
}
