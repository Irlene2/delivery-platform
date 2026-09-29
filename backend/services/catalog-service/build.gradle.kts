plugins {
    id("delivery.mongo-conventions")
    id("delivery.messaging-conventions")
    // delivery.redis-conventions saiu na G-B1 (26/09/2026), pela mesma
    // conferência que o tirou do merchant, do order e do delivery.
    //
    // A diferença deste caso: aqui o Redis TEM motivo escrito — a §7 do
    // catalogo.md manda cachear o cardápio público, com TTL curto e
    // invalidação por evento. O motivo continua de pé; o que sai é a
    // dependência. Quem lê o cardápio público é o marco 7, e até lá o starter
    // só registra um indicador de saúde que não acha servidor nenhum — foi
    // exatamente assim que o /actuator/health do merchant começou a responder
    // 503 e alguém desligou o indicador, que é a pior saída.
    //
    // GATILHO ESCRITO: o starter volta junto com o primeiro leitor do cardápio
    // público em cache, no marco 7 — e volta com contêiner nos testes de
    // integração, não com indicador desligado.
}

dependencies {
    // A única dependência de módulo do repositório que um serviço pode declarar.
    // A ADR-001, emendada pela ADR-040, diz: build-logic, :value-types e
    // bibliotecas externas -- e nada mais. Um segundo project(...) aqui é
    // violação da ADR-001, não uma linha a mais.
    implementation(project(":value-types"))

    // O cache de autorização da ADR-011, em processo. Sem versão: o BOM do
    // Spring Boot a gerencia. Sem o spring-boot-starter-cache: não há
    // @Cacheable aqui, só um Cache montado à mão (ADR-048).
    implementation("com.github.ben-manes.caffeine:caffeine")
}
