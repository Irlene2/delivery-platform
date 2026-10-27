// Sem redis-conventions: nada no order usa Redis, e nada escrito jamais disse por
// quê. Os cinco usos que a arquitetura v1.0 atribuía ao Redis morreram cada um
// numa decisão própria. ADR-021, emenda de 26/09/2026.
plugins {
    id("delivery.jpa-conventions")
    id("delivery.messaging-conventions")
}

dependencies {
    // A única dependência de módulo que um serviço pode declarar (ADR-001, emendada
    // pela ADR-040). O pedido calcula em Money: subtotal, taxa, total e troco.
    implementation(project(":value-types"))
}
