-- O caixa do dia, fechado e guardado (ADR-077, emenda de 01/11/2026). Apurado pelo order a
-- partir dos pedidos do dia; uma vez por loja e dia.
CREATE TABLE fechamento_do_dia (
    id                  UUID PRIMARY KEY,
    estabelecimento_id  UUID          NOT NULL,
    dia                 DATE          NOT NULL,
    fechado_em          TIMESTAMPTZ   NOT NULL,
    fechado_por         UUID          NOT NULL,
    pedidos             INTEGER       NOT NULL,
    concluidos          INTEGER       NOT NULL,
    cancelados          INTEGER       NOT NULL,
    vendas              NUMERIC(19,2) NOT NULL,
    taxas               NUMERIC(19,2) NOT NULL,
    dinheiro            NUMERIC(19,2) NOT NULL,
    cartao              NUMERIC(19,2) NOT NULL,
    pix                 NUMERIC(19,2) NOT NULL,
    -- A conferência: o dinheiro contado no caixa, quando alguém contou.
    dinheiro_contado    NUMERIC(19,2),
    observacao          VARCHAR(500),

    CONSTRAINT uq_fechamento_loja_dia UNIQUE (estabelecimento_id, dia),
    CONSTRAINT ck_fechamento_contagens CHECK (pedidos = concluidos + cancelados),
    CONSTRAINT ck_fechamento_formas CHECK (vendas = dinheiro + cartao + pix),
    CONSTRAINT ck_fechamento_contado CHECK (dinheiro_contado IS NULL OR dinheiro_contado >= 0)
);

-- O acerto de cada entregador no dia. entregador_id nulo = o avulso, só pelo nome.
CREATE TABLE acerto_do_fechamento (
    fechamento_id   UUID          NOT NULL REFERENCES fechamento_do_dia (id),
    ordem           INTEGER       NOT NULL,
    entregador_id   UUID,
    nome            VARCHAR(80)   NOT NULL,
    entregas        INTEGER       NOT NULL,
    repasse         NUMERIC(19,2) NOT NULL,
    dinheiro        NUMERIC(19,2) NOT NULL,
    cartao          NUMERIC(19,2) NOT NULL,

    PRIMARY KEY (fechamento_id, ordem)
);
