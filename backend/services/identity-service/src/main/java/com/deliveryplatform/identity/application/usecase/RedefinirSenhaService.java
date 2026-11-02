package com.deliveryplatform.identity.application.usecase;

import com.deliveryplatform.identity.application.exception.RedefinicaoRecusada;
import com.deliveryplatform.identity.application.port.in.RedefinirSenha;
import com.deliveryplatform.identity.application.port.out.CodificadorDeSenha;
import com.deliveryplatform.identity.application.port.out.CodigoDeVerificacaoRepositorio;
import com.deliveryplatform.identity.application.port.out.GeradorDeCodigo;
import com.deliveryplatform.identity.application.port.out.UsuarioRepositorio;
import com.deliveryplatform.identity.domain.model.CodigoDeVerificacao;
import com.deliveryplatform.identity.domain.model.Telefone;
import com.deliveryplatform.identity.domain.model.Usuario;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Esqueci minha senha, pelo código de verificação (02/11/2026).
 *
 * <p><b>Reaproveita a tabela do código do cadastro</b>, e as duas não se misturam: o código do
 * cadastro só nasce para telefone <i>sem</i> conta, e o desta recuperação só para telefone
 * <i>com</i> conta. Validade, limite de tentativas e o código de seis dígitos são os mesmos
 * ({@link CodigoDeVerificacao}).
 *
 * <p><b>Como o código chega à pessoa é o mesmo problema do cadastro</b>: até haver envio por SMS
 * ou WhatsApp, ele aparece na tela só com a bandeira de desenvolvimento (ADR-042, emenda).
 */
@Service
public class RedefinirSenhaService implements RedefinirSenha {

    private final UsuarioRepositorio usuarios;
    private final CodigoDeVerificacaoRepositorio codigos;
    private final GeradorDeCodigo gerador;
    private final CodificadorDeSenha codificador;
    private final Clock relogio;

    public RedefinirSenhaService(UsuarioRepositorio usuarios, CodigoDeVerificacaoRepositorio codigos,
                                 GeradorDeCodigo gerador, CodificadorDeSenha codificador,
                                 Clock relogio) {
        this.usuarios = usuarios;
        this.codigos = codigos;
        this.gerador = gerador;
        this.codificador = codificador;
        this.relogio = relogio;
    }

    @Override
    public Optional<String> solicitarCodigo(String telefoneBruto) {
        Telefone telefone = Telefone.de(telefoneBruto);
        if (!usuarios.existeComTelefone(telefone)) {
            return Optional.empty();
        }
        String codigo = gerador.gerar();
        codigos.substituir(CodigoDeVerificacao.novo(telefone, codigo, relogio.instant()));
        return Optional.of(codigo);
    }

    /**
     * <b>Não é transacional, pelo mesmo motivo do {@code CadastrarUsuarioService}</b>: a
     * tentativa errada precisa ficar gravada, e um rollback junto com a recusa desfaria o
     * contador — o limite de tentativas nunca chegaria, e seis dígitos cairiam por força bruta.
     */
    @Override
    public void redefinir(String telefoneBruto, String codigoInformado, String novaSenha) {
        Telefone telefone = Telefone.de(telefoneBruto);
        Instant agora = relogio.instant();

        CodigoDeVerificacao codigo = codigos.buscarPorTelefone(telefone)
                .orElseThrow(RedefinicaoRecusada::new);
        if (!codigo.confere(codigoInformado, agora)) {
            // Guarda a tentativa: o limite do código vale aqui como no cadastro.
            codigos.substituir(codigo);
            throw new RedefinicaoRecusada();
        }
        Usuario usuario = usuarios.buscarPorTelefone(telefone).orElseThrow(RedefinicaoRecusada::new);

        usuarios.salvar(usuario.comSenha(codificador.codificar(novaSenha)));
        codigos.removerDe(telefone);
    }
}
