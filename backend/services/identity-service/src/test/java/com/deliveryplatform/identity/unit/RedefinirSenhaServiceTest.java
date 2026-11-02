package com.deliveryplatform.identity.unit;

import com.deliveryplatform.identity.application.exception.RedefinicaoRecusada;
import com.deliveryplatform.identity.application.port.out.CodificadorDeSenha;
import com.deliveryplatform.identity.application.port.out.CodigoDeVerificacaoRepositorio;
import com.deliveryplatform.identity.application.port.out.GeradorDeCodigo;
import com.deliveryplatform.identity.application.port.out.UsuarioRepositorio;
import com.deliveryplatform.identity.application.usecase.RedefinirSenhaService;
import com.deliveryplatform.identity.domain.model.CodigoDeVerificacao;
import com.deliveryplatform.identity.domain.model.Telefone;
import com.deliveryplatform.identity.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Esqueci minha senha (02/11/2026): o código do cadastro, ao contrário. */
@ExtendWith(MockitoExtension.class)
class RedefinirSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-02T12:00:00Z");
    private static final Telefone CANONICO = new Telefone("+5511987654321");
    private static final String BRUTO = "11987654321";
    private static final String CERTO = "004291";

    @Mock
    UsuarioRepositorio usuarios;

    @Mock
    CodigoDeVerificacaoRepositorio codigos;

    @Mock
    GeradorDeCodigo gerador;

    @Mock
    CodificadorDeSenha codificador;

    RedefinirSenhaService servico;

    @BeforeEach
    void montar() {
        servico = new RedefinirSenhaService(usuarios, codigos, gerador, codificador,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private Usuario marli() {
        return Usuario.novo("Marli", CANONICO, AGORA.minusSeconds(86_400), "{bcrypt}$antiga");
    }

    @Test
    void telefone_com_conta_ganha_codigo() {
        when(usuarios.existeComTelefone(CANONICO)).thenReturn(true);
        when(gerador.gerar()).thenReturn(CERTO);

        assertThat(servico.solicitarCodigo(BRUTO)).contains(CERTO);
        verify(codigos).substituir(any(CodigoDeVerificacao.class));
    }

    @Test
    void telefone_sem_conta_nao_ganha_codigo_e_nada_e_gravado() {
        when(usuarios.existeComTelefone(CANONICO)).thenReturn(false);

        assertThat(servico.solicitarCodigo(BRUTO)).isEmpty();
        verifyNoInteractions(codigos, gerador);
    }

    @Test
    void o_codigo_certo_troca_a_senha_e_o_codigo_morre() {
        Usuario usuario = marli();
        when(codigos.buscarPorTelefone(CANONICO))
                .thenReturn(Optional.of(CodigoDeVerificacao.novo(CANONICO, CERTO, AGORA)));
        when(usuarios.buscarPorTelefone(CANONICO)).thenReturn(Optional.of(usuario));
        when(codificador.codificar("senha-nova-123")).thenReturn("{bcrypt}$nova");

        servico.redefinir(BRUTO, CERTO, "senha-nova-123");

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).salvar(salvo.capture());
        assertThat(salvo.getValue().getId()).isEqualTo(usuario.getId());
        assertThat(salvo.getValue().getHashDaSenha()).isEqualTo("{bcrypt}$nova");
        verify(codigos).removerDe(CANONICO);
    }

    @Test
    void o_codigo_errado_recusa_guarda_a_tentativa_e_nao_toca_na_senha() {
        CodigoDeVerificacao codigo = CodigoDeVerificacao.novo(CANONICO, CERTO, AGORA);
        when(codigos.buscarPorTelefone(CANONICO)).thenReturn(Optional.of(codigo));

        assertThatThrownBy(() -> servico.redefinir(BRUTO, "999999", "senha-nova-123"))
                .isInstanceOf(RedefinicaoRecusada.class);

        verify(codigos).substituir(codigo);
        assertThat(codigo.getTentativas()).isEqualTo(1);
        verify(usuarios, never()).salvar(any());
    }

    @Test
    void sem_codigo_pedido_e_a_mesma_recusa() {
        when(codigos.buscarPorTelefone(CANONICO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servico.redefinir(BRUTO, CERTO, "senha-nova-123"))
                .isInstanceOf(RedefinicaoRecusada.class);
        verify(usuarios, never()).salvar(any());
    }
}
