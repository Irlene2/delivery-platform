package com.deliveryplatform.merchant.unit;

import com.deliveryplatform.merchant.api.dto.CriarEstabelecimentoRequest.IdentificacaoRequest;
import com.deliveryplatform.merchant.domain.model.FusoHorario;
import io.swagger.v3.oas.annotations.media.Schema;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * As zonas que o contrato declara são as que o domínio aceita (W-E, ADR-061 §4).
 *
 * <p>O {@code @Schema(allowableValues)} do {@code fusoHorario} é uma segunda cópia do
 * conjunto da M16 — e é aceita <b>só</b> porque este teste a solda à primeira. O front lê
 * o tipo gerado do contrato; se as duas listas divergissem, a tela ofereceria uma zona
 * que o agregado recusa, ou esconderia uma que ele aceita.
 *
 * <p>Lê o campo, e não o componente do record: o {@code @Target} do {@code @Schema} não
 * inclui {@code RECORD_COMPONENT}, e a anotação é propagada para o campo privado.
 */
class AllowableValuesDoFusoTest {

    private static Schema schemaDoFuso() throws NoSuchFieldException {
        Schema schema = IdentificacaoRequest.class.getDeclaredField("fusoHorario").getAnnotation(Schema.class);
        assertThat(schema).as("o campo fusoHorario perdeu o @Schema").isNotNull();
        return schema;
    }

    @Test
    void os_valores_permitidos_sao_exatamente_os_do_dominio() throws Exception {
        Set<String> doContrato = Set.of(schemaDoFuso().allowableValues());

        assertThat(doContrato).hasSize(16);
        assertThat(doContrato).isEqualTo(FusoHorario.identificadoresAceitos());
    }

    @Test
    void o_padrao_e_o_do_dominio() throws Exception {
        assertThat(schemaDoFuso().defaultValue()).isEqualTo(FusoHorario.PADRAO.identificador());
    }
}
