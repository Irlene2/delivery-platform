-- O entregador da equipe (decisão de 31/10/2026): ele entra com a conta dele e vê as
-- entregas que a loja lhe passou.

-- Quem tem conta. Nulo = um entregador só pelo nome (o motoboy avulso), como era até a V1.
ALTER TABLE pedido ADD COLUMN entregador_id UUID;

-- "As minhas entregas de hoje": a loja, o entregador, o dia.
CREATE INDEX idx_pedido_loja_entregador_criado_em
    ON pedido (estabelecimento_id, entregador_id, criado_em DESC)
    WHERE entregador_id IS NOT NULL;

-- O nome com que a loja reconhece cada entregador. A equipe do merchant não guarda nome, e
-- a entrega de 15/11 não mexe nela (ADR-077): o próprio entregador se apresenta aqui.
CREATE TABLE entregador (
    estabelecimento_id  UUID NOT NULL,
    usuario_id          UUID NOT NULL,
    nome                VARCHAR(80) NOT NULL,

    PRIMARY KEY (estabelecimento_id, usuario_id)
);
