// Redis: NENHUM serviço aplica este plugin desde a G-B1 (26/09/2026).
//
// O único uso com motivo escrito é o cache do cardápio público do catalog —
// ADR-005, "O Redis fica, para cache", e catalogo.md §7 —, e o leitor desse
// cache é do marco 7. Até lá o starter só registraria um indicador de saúde
// sem servidor. Os outros usos que este cabeçalho já listou (TTL de carrinho,
// idempotência, GEO, presença) morreram cada um numa decisão própria; a tabela
// está na ADR-021.
//
// O plugin fica porque o gatilho de volta está escrito: o primeiro leitor do
// cardápio em cache, no marco 7. Serviço que o aplicar aponta o porquê na
// coluna "Por quê" da ADR-021, ou o starter vira dependência sem dono.

plugins {
    id("delivery.spring-service-conventions")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Testcontainers 2.x prefixa os módulos com "testcontainers-".
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
}
