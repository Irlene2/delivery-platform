-- O pedido da entrega de 15/11 (ADR-077, docs/entrega-15-11/pedido.md).
--
-- Valores em NUMERIC(19,2), como manda a ADR-009: o Money do :value-types tem escala 2, e o banco
-- guarda o que o domínio calculou — nunca recalcula.

CREATE TABLE pedido (
    id                        UUID PRIMARY KEY,
    estabelecimento_id        UUID NOT NULL,
    numero                    BIGINT NOT NULL,
    codigo_de_acompanhamento  UUID NOT NULL,

    cliente_nome              VARCHAR(120) NOT NULL,
    -- E.164, normalizado no domínio como no identity.
    cliente_telefone          VARCHAR(16)  NOT NULL,

    modalidade                VARCHAR(16)  NOT NULL,
    endereco_logradouro       VARCHAR(200),
    endereco_bairro           VARCHAR(80),
    endereco_complemento      VARCHAR(80),

    subtotal                  NUMERIC(19,2) NOT NULL,
    taxa_de_entrega           NUMERIC(19,2) NOT NULL,
    total                     NUMERIC(19,2) NOT NULL,

    forma_declarada           VARCHAR(16)  NOT NULL,
    troco_para                NUMERIC(19,2),

    status                    VARCHAR(24)  NOT NULL,
    motivo_do_cancelamento    VARCHAR(200),
    entregador                VARCHAR(80),

    -- O pagamento registrado nasce inteiro ou não nasce (pedido.md §5).
    forma_efetiva             VARCHAR(16),
    valor_recebido            NUMERIC(19,2),
    troco_devolvido           NUMERIC(19,2),
    pix_conferido             BOOLEAN,
    pagamento_registrado_em   TIMESTAMPTZ,

    criado_em                 TIMESTAMPTZ NOT NULL,
    atualizado_em             TIMESTAMPTZ NOT NULL,
    versao                    BIGINT NOT NULL,

    CONSTRAINT uq_pedido_numero_por_loja UNIQUE (estabelecimento_id, numero),
    CONSTRAINT uq_pedido_codigo UNIQUE (codigo_de_acompanhamento),
    CONSTRAINT ck_pedido_total CHECK (total = subtotal + taxa_de_entrega),
    -- Entrega tem endereço; retirada não tem.
    CONSTRAINT ck_pedido_endereco CHECK (
        (modalidade = 'ENTREGA' AND endereco_logradouro IS NOT NULL AND endereco_bairro IS NOT NULL)
        OR (modalidade = 'RETIRADA' AND endereco_logradouro IS NULL)),
    CONSTRAINT ck_pedido_cancelado_tem_motivo
        CHECK ((status = 'CANCELADO') = (motivo_do_cancelamento IS NOT NULL)),
    CONSTRAINT ck_pedido_pagamento_inteiro
        CHECK ((forma_efetiva IS NULL) = (pagamento_registrado_em IS NULL))
);

-- A lista do painel: a loja, o dia, do mais novo para o mais velho.
CREATE INDEX idx_pedido_loja_criado_em ON pedido (estabelecimento_id, criado_em DESC);

CREATE TABLE item_do_pedido (
    pedido_id        UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    posicao          INTEGER NOT NULL,
    produto_id       UUID NOT NULL,
    -- Nome e preço CONGELADOS no instante do pedido (PRD H4.2): mudar o
    -- cardápio depois não muda o que o cliente pediu.
    nome             VARCHAR(120) NOT NULL,
    preco_unitario   NUMERIC(19,2) NOT NULL CHECK (preco_unitario >= 0),
    quantidade       INTEGER NOT NULL CHECK (quantidade BETWEEN 1 AND 99),
    observacao       VARCHAR(140),

    PRIMARY KEY (pedido_id, posicao)
);

-- A taxa fixa e as modalidades (decisão de 26/10/2026). Sem linha = o padrão:
-- taxa zero, as duas modalidades.
CREATE TABLE configuracao_de_pedidos (
    estabelecimento_id  UUID PRIMARY KEY,
    taxa_de_entrega     NUMERIC(19,2) NOT NULL CHECK (taxa_de_entrega >= 0),
    aceita_entrega      BOOLEAN NOT NULL,
    aceita_retirada     BOOLEAN NOT NULL,

    CONSTRAINT ck_configuracao_alguma_modalidade CHECK (aceita_entrega OR aceita_retirada)
);

-- O número legível por loja (000001, 000002…). Uma linha por loja, travada pelo
-- UPDATE até o fim da transação que cria o pedido: dois pedidos simultâneos
-- nunca recebem o mesmo número. SEQUENCE do banco seria global, não por loja.
CREATE TABLE numeracao_de_pedido (
    estabelecimento_id  UUID PRIMARY KEY,
    ultimo              BIGINT NOT NULL
);
