package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.application.exception.CadastroDeLojaRecusado;
import com.deliveryplatform.merchant.application.exception.CadastroDeLojaRecusado.Motivo;
import com.deliveryplatform.merchant.application.port.in.CriarEstabelecimento.NovoEstabelecimento;
import com.deliveryplatform.merchant.application.port.in.LojaDoUsuario;
import com.deliveryplatform.merchant.application.port.out.EstabelecimentoRepositorio;
import com.deliveryplatform.merchant.application.port.out.MembroRepositorio;
import com.deliveryplatform.merchant.application.usecase.CriarEstabelecimentoService;
import com.deliveryplatform.merchant.application.usecase.GerenciarEquipeService;
import com.deliveryplatform.merchant.domain.model.Estabelecimento;
import com.deliveryplatform.merchant.domain.model.EstadoDoMembro;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import com.deliveryplatform.merchant.domain.model.Membro;
import com.deliveryplatform.merchant.domain.model.MetodoPagamento;
import com.deliveryplatform.merchant.domain.model.Modalidade;
import com.deliveryplatform.merchant.domain.model.Papel;
import com.deliveryplatform.merchant.domain.model.Permissao;
import com.deliveryplatform.merchant.domain.model.TipoDeOperacao;
import com.deliveryplatform.valuetypes.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A criação da loja (ADR-060). O caso 5 é o que a rodada existe para ter: os
 * quatro primeiros são verdadeiros com e sem o evento, e por isso não provam nada
 * sobre ele.
 *
 * <p>O que este arquivo <b>não</b> prova: que a transação é uma só, e que a ordem
 * importa para o banco. Um dublê não tem transação nem chave estrangeira — isso é
 * do {@code CriarEstabelecimentoIT}.
 */
@ExtendWith(MockitoExtension.class)
class CriarEstabelecimentoServiceTest {

    private static final UUID PORTADOR = UUID.fromString("0a0a0a0a-0000-4000-8000-000000000001");
    private static final Instant AGORA = Instant.parse("2026-10-10T15:30:00.123456Z");

    @Mock
    EstabelecimentoRepositorio estabelecimentos;

    @Mock
    MembroRepositorio membros;

    @Mock
    GerenciarEquipeService equipes;

    private CriarEstabelecimentoService servico() {
        return new CriarEstabelecimentoService(
                estabelecimentos, membros, equipes, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    /** Duas modalidades: com uma só, um mapa de chave fixa passaria igual (caso 3). */
    private static NovoEstabelecimento pedido() {
        return new NovoEstabelecimento(
                "Pizzaria da Marli", "12.345.678/0001-95", "(11) 98765-4321",
                "Rua das Palmeiras, 100", "Boa Viagem", "America/Recife",
                new BigDecimal("50.00"), true,
                TipoDeOperacao.PRODUCAO,
                Map.of(Modalidade.ENTREGA, Set.of(MetodoPagamento.DINHEIRO, MetodoPagamento.PIX),
                        Modalidade.RETIRADA, Set.of(MetodoPagamento.CARTAO)));
    }

    private LojaDoUsuario criar() {
        when(estabelecimentos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        when(membros.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
        return servico().criar(PORTADOR, pedido());
    }

    private Estabelecimento lojaSalva() {
        ArgumentCaptor<Estabelecimento> loja = ArgumentCaptor.forClass(Estabelecimento.class);
        verify(estabelecimentos).salvar(loja.capture());
        return loja.getValue();
    }

    private Membro membroSalvo() {
        ArgumentCaptor<Membro> membro = ArgumentCaptor.forClass(Membro.class);
        verify(membros).salvar(membro.capture());
        return membro.getValue();
    }

    @Test
    void caso1_cria_a_loja_com_os_valores_do_pedido() {
        criar();

        Estabelecimento loja = lojaSalva();
        assertThat(loja.getIdentificacao().nome()).isEqualTo("Pizzaria da Marli");
        assertThat(loja.getIdentificacao().documento().numero()).isEqualTo("12345678000195");
        assertThat(loja.getIdentificacao().fusoHorario()).isEqualTo(FusoHorario.de("America/Recife"));
        assertThat(loja.getPoliticaDeTroco().fundoMaximoDeTroco()).isEqualTo(Money.de("50.00"));
        assertThat(loja.getPoliticaDeTroco().aceitaPedidoSemTrocoDisponivel()).isTrue();
        assertThat(loja.getOperacao().tipoDeOperacao()).isEqualTo(TipoDeOperacao.PRODUCAO);
        assertThat(loja.metodosDe(Modalidade.ENTREGA))
                .containsExactlyInAnyOrder(MetodoPagamento.DINHEIRO, MetodoPagamento.PIX);
    }

    @Test
    void caso2_a_loja_nasce_sem_horario_e_sem_area() {
        criar();

        Estabelecimento loja = lojaSalva();
        for (DayOfWeek dia : DayOfWeek.values()) {
            assertThat(loja.getDisponibilidade().faixasDe(dia)).isEmpty();
        }
        assertThat(loja.estaAberta(AGORA)).isFalse();
        assertThat(loja.getAreasDeEntrega()).isEmpty();
    }

    @Test
    void caso3_minimos_e_desconto_nascem_zero_e_as_chaves_sao_as_dos_metodos() {
        criar();

        var operacao = lojaSalva().getOperacao();
        assertThat(operacao.pedidoMinimoPorModalidade())
                .containsOnlyKeys(Modalidade.ENTREGA, Modalidade.RETIRADA)
                .allSatisfy((modalidade, minimo) -> assertThat(minimo).isEqualTo(Money.ZERO));
        assertThat(operacao.descontoDeRetirada()).isEqualTo(Money.ZERO);
    }

    @Test
    void caso4_o_fundador_e_o_portador_e_e_da_loja_salva() {
        LojaDoUsuario resposta = criar();

        Membro dono = membroSalvo();
        UUID loja = lojaSalva().getId();
        assertThat(dono.getUsuarioId()).isEqualTo(PORTADOR);
        assertThat(dono.getEstabelecimentoId()).isEqualTo(loja);
        assertThat(dono.getPapel()).isEqualTo(Papel.ADMINISTRADOR);
        assertThat(dono.getEstado()).isEqualTo(EstadoDoMembro.ATIVO);
        assertThat(dono.pode(Permissao.VER_PRODUTO)).isTrue();
        assertThat(dono.pode(Permissao.GERENCIAR_EQUIPE)).isTrue();

        assertThat(resposta.estabelecimentoId()).isEqualTo(loja);
        assertThat(resposta.nome()).isEqualTo("Pizzaria da Marli");
        assertThat(resposta.papel()).isEqualTo(Papel.ADMINISTRADOR);
        assertThat(resposta.permissoes()).containsExactlyInAnyOrder(Permissao.values());
    }

    @Test
    void caso5_o_vinculo_nascido_vai_ao_outbox_com_o_mesmo_membro_e_o_mesmo_instante() {
        criar();

        Membro salvo = membroSalvo();
        verify(equipes).registrarVinculoNascido(salvo, AGORA);
    }

    @Test
    void caso6_a_loja_e_salva_antes_do_vinculo() {
        criar();

        InOrder ordem = inOrder(estabelecimentos, membros, equipes);
        ordem.verify(estabelecimentos).salvar(any());
        ordem.verify(membros).salvar(any());
        ordem.verify(equipes).registrarVinculoNascido(any(), any());
    }

    /** O pedido válido, com um campo trocado — cada caso abaixo varia um só. */
    private static NovoEstabelecimento com(String documento, String telefone, String fuso,
                                           String nome, BigDecimal fundo,
                                           Map<Modalidade, Set<MetodoPagamento>> metodos) {
        return new NovoEstabelecimento(nome, documento, telefone, "Rua A, 1", "Centro", fuso,
                fundo, false, TipoDeOperacao.PRODUCAO, metodos);
    }

    private static final Map<Modalidade, Set<MetodoPagamento>> RETIRADA_EM_DINHEIRO =
            Map.of(Modalidade.RETIRADA, Set.of(MetodoPagamento.DINHEIRO));

    /**
     * O censo dos motivos (W-E): cada parte do agregado, e o que ela recusa. O motivo
     * é o que o tratador lê para escrever a frase — e a causa guarda a mensagem do
     * agregado, que não vai à tela.
     */
    static Stream<Arguments> recusas() {
        return Stream.of(
                Arguments.of(Motivo.DOCUMENTO,
                        com("123", "(11) 98765-4321", "America/Sao_Paulo", "Pizzaria", BigDecimal.ZERO, RETIRADA_EM_DINHEIRO)),
                Arguments.of(Motivo.TELEFONE,
                        com("12345678901", "não é telefone", "America/Sao_Paulo", "Pizzaria", BigDecimal.ZERO, RETIRADA_EM_DINHEIRO)),
                Arguments.of(Motivo.FUSO_HORARIO,
                        com("12345678901", "(11) 98765-4321", "Europe/Lisbon", "Pizzaria", BigDecimal.ZERO, RETIRADA_EM_DINHEIRO)),
                // Inalcançável pela borda: o @NotBlank do DTO recusa antes. Prova-se aqui.
                Arguments.of(Motivo.TEXTO_OBRIGATORIO,
                        com("12345678901", "(11) 98765-4321", "America/Sao_Paulo", "   ", BigDecimal.ZERO, RETIRADA_EM_DINHEIRO)),
                Arguments.of(Motivo.MODALIDADE_SEM_FORMA_DE_PAGAR,
                        com("12345678901", "(11) 98765-4321", "America/Sao_Paulo", "Pizzaria", BigDecimal.ZERO, Map.of())),
                Arguments.of(Motivo.MODALIDADE_SEM_FORMA_DE_PAGAR,
                        com("12345678901", "(11) 98765-4321", "America/Sao_Paulo", "Pizzaria", BigDecimal.ZERO,
                                Map.of(Modalidade.ENTREGA, Set.of()))),
                Arguments.of(Motivo.FUNDO_DE_TROCO_NEGATIVO,
                        com("12345678901", "(11) 98765-4321", "America/Sao_Paulo", "Pizzaria", new BigDecimal("-1.00"), RETIRADA_EM_DINHEIRO)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("recusas")
    void cada_recusa_do_agregado_tem_o_seu_motivo_e_nada_e_salvo(Motivo esperado, NovoEstabelecimento pedido) {
        assertThatThrownBy(() -> servico().criar(PORTADOR, pedido))
                .isInstanceOfSatisfying(CadastroDeLojaRecusado.class,
                        recusa -> assertThat(recusa.motivo()).isEqualTo(esperado))
                .hasCauseInstanceOf(RuntimeException.class);
        verifyNoInteractions(estabelecimentos, membros, equipes);
    }
}
