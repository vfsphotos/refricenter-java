-- ============================================================
-- RefriCenter — Schema inicial para PostgreSQL
-- Flyway V1 — gerado a partir dos models Django
-- ============================================================

-- Usuários
CREATE TABLE IF NOT EXISTS usuario (
    id           BIGSERIAL PRIMARY KEY,
    username     VARCHAR(150) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    first_name   VARCHAR(150) DEFAULT '',
    last_name    VARCHAR(150) DEFAULT '',
    email        VARCHAR(254) DEFAULT '',
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,
    is_superuser BOOLEAN NOT NULL DEFAULT FALSE
);

-- Plataforma SaaS (configuração global)
CREATE TABLE IF NOT EXISTS plataforma_config (
    id                    BIGSERIAL PRIMARY KEY,
    manutencao_ativa      BOOLEAN NOT NULL DEFAULT FALSE,
    mensagem_manutencao   TEXT DEFAULT 'Sistema em manutenção.',
    nome_plataforma       VARCHAR(100) DEFAULT 'RefriCenter SaaS'
);
INSERT INTO plataforma_config (manutencao_ativa) VALUES (false) ON CONFLICT DO NOTHING;

-- SuperAdmin
CREATE TABLE IF NOT EXISTS super_admin (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    nivel            VARCHAR(20) NOT NULL DEFAULT 'suporte',
    ativo            BOOLEAN NOT NULL DEFAULT TRUE,
    pode_impersonar  BOOLEAN NOT NULL DEFAULT FALSE
);

-- Empresa (Tenant)
CREATE TABLE IF NOT EXISTS empresa (
    id                  BIGSERIAL PRIMARY KEY,
    nome                VARCHAR(200) NOT NULL,
    cnpj                VARCHAR(18) DEFAULT '',
    slug                VARCHAR(60) NOT NULL UNIQUE,
    logo_path           VARCHAR(500),
    ativa               BOOLEAN NOT NULL DEFAULT TRUE,
    plano               VARCHAR(20) NOT NULL DEFAULT 'profissional',
    fiscal_habilitado   BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao        TIMESTAMP NOT NULL DEFAULT NOW(),
    dono_id             BIGINT NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT
);
CREATE INDEX IF NOT EXISTS idx_empresa_slug ON empresa(slug);

-- Setor
CREATE TABLE IF NOT EXISTS setor (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    nome        VARCHAR(100) NOT NULL,
    descricao   VARCHAR(200) DEFAULT '',
    cor         VARCHAR(7) DEFAULT '#3b82f6',
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (empresa_id, nome)
);

-- Permissão de setor
CREATE TABLE IF NOT EXISTS permissao_setor (
    id              BIGSERIAL PRIMARY KEY,
    setor_id        BIGINT NOT NULL REFERENCES setor(id) ON DELETE CASCADE,
    modulo          VARCHAR(30) NOT NULL,
    pode_visualizar BOOLEAN NOT NULL DEFAULT TRUE,
    pode_criar      BOOLEAN NOT NULL DEFAULT FALSE,
    pode_editar     BOOLEAN NOT NULL DEFAULT FALSE,
    pode_deletar    BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (setor_id, modulo)
);

-- Usuário da Empresa
CREATE TABLE IF NOT EXISTS usuario_empresa (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL UNIQUE REFERENCES usuario(id) ON DELETE CASCADE,
    empresa_id    BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    setor_id      BIGINT REFERENCES setor(id) ON DELETE SET NULL,
    papel         VARCHAR(20) NOT NULL DEFAULT 'tecnico',
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_ue_empresa ON usuario_empresa(empresa_id);

-- Configuração do Sistema (uma por empresa)
CREATE TABLE IF NOT EXISTS configuracao_sistema (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT UNIQUE REFERENCES empresa(id) ON DELETE CASCADE,
    nome_empresa          VARCHAR(200) DEFAULT 'MagnoosSystem',
    cnpj_empresa          VARCHAR(18) DEFAULT '',
    endereco              VARCHAR(300) DEFAULT '',
    numero_endereco       VARCHAR(10) DEFAULT '',
    bairro                VARCHAR(100) DEFAULT '',
    cidade                VARCHAR(100) DEFAULT '',
    estado                VARCHAR(2) DEFAULT '',
    cep                   VARCHAR(10) DEFAULT '',
    telefone              VARCHAR(20) DEFAULT '',
    email                 VARCHAR(254) DEFAULT '',
    horario_funcionamento VARCHAR(200) DEFAULT '',
    logo_path             VARCHAR(500),
    observacoes_nota      TEXT DEFAULT '',
    mensagem_boas_vindas  TEXT DEFAULT 'Bem-vindo ao MagnoosSystem!',
    data_atualizacao      TIMESTAMP DEFAULT NOW(),
    numero_inicial_os     INTEGER DEFAULT 1,
    ultima_os             INTEGER DEFAULT 0,
    caixa_inicial         NUMERIC(10,2) DEFAULT 0
);

-- Marca
CREATE TABLE IF NOT EXISTS marca (
    id           BIGSERIAL PRIMARY KEY,
    nome         VARCHAR(100) NOT NULL UNIQUE,
    descricao    TEXT DEFAULT '',
    logo_path    VARCHAR(500),
    ativa        BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Categoria de Produto
CREATE TABLE IF NOT EXISTS categoria_produto (
    id        BIGSERIAL PRIMARY KEY,
    nome      VARCHAR(100) NOT NULL UNIQUE,
    descricao TEXT DEFAULT ''
);

-- Produto
CREATE TABLE IF NOT EXISTS produto (
    id             BIGSERIAL PRIMARY KEY,
    empresa_id     BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    codigo         VARCHAR(50) NOT NULL,
    nome           VARCHAR(200) NOT NULL,
    descricao      TEXT DEFAULT '',
    categoria_id   BIGINT REFERENCES categoria_produto(id) ON DELETE SET NULL,
    marca_id       BIGINT REFERENCES marca(id) ON DELETE SET NULL,
    preco_custo    NUMERIC(10,2) DEFAULT 0,
    preco_venda    NUMERIC(10,2) DEFAULT 0,
    estoque_minimo INTEGER DEFAULT 0,
    estoque_atual  INTEGER DEFAULT 0,
    localizacao    VARCHAR(100) DEFAULT '',
    ativo          BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro  TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (codigo, empresa_id)
);
CREATE INDEX IF NOT EXISTS idx_produto_empresa ON produto(empresa_id);
CREATE INDEX IF NOT EXISTS idx_produto_ativo ON produto(ativo);
CREATE INDEX IF NOT EXISTS idx_produto_estoque ON produto(estoque_atual);

-- Cliente
CREATE TABLE IF NOT EXISTS cliente (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    nome          VARCHAR(200) NOT NULL,
    tipo          VARCHAR(2) DEFAULT 'PF',
    cpf_cnpj      VARCHAR(18) DEFAULT '',
    telefone      VARCHAR(20) DEFAULT '',
    whatsapp      VARCHAR(20) DEFAULT '',
    email         VARCHAR(254) DEFAULT '',
    endereco      VARCHAR(300) DEFAULT '',
    numero        VARCHAR(10) DEFAULT '',
    complemento   VARCHAR(100) DEFAULT '',
    bairro        VARCHAR(100) DEFAULT '',
    cidade        VARCHAR(100) DEFAULT '',
    estado        VARCHAR(2) DEFAULT '',
    cep           VARCHAR(10) DEFAULT '',
    observacoes   TEXT DEFAULT '',
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),
    criado_por_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_cliente_empresa ON cliente(empresa_id);
CREATE INDEX IF NOT EXISTS idx_cliente_ativo ON cliente(ativo);
CREATE INDEX IF NOT EXISTS idx_cliente_nome ON cliente(nome);

-- Equipamento
CREATE TABLE IF NOT EXISTS equipamento (
    id                    BIGSERIAL PRIMARY KEY,
    cliente_id            BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    marca_id              BIGINT REFERENCES marca(id) ON DELETE SET NULL,
    modelo                VARCHAR(200) NOT NULL,
    numero_serie          VARCHAR(100) DEFAULT '',
    potencia              VARCHAR(50) DEFAULT '',
    tensao                VARCHAR(10) DEFAULT '',
    defeito_reclamado     TEXT NOT NULL,
    acessorios            TEXT DEFAULT '',
    estado_conserto       VARCHAR(20) DEFAULT 'recebido',
    observacoes_tecnicas  TEXT DEFAULT '',
    data_recebimento      TIMESTAMP NOT NULL DEFAULT NOW(),
    data_prevista_entrega TIMESTAMP,
    data_entrega          TIMESTAMP,
    foto_path             VARCHAR(500),
    ativo                 BOOLEAN NOT NULL DEFAULT TRUE
);

-- Ordem de Serviço
CREATE TABLE IF NOT EXISTS ordem_servico (
    id                      BIGSERIAL PRIMARY KEY,
    empresa_id              BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    numero                  VARCHAR(20) NOT NULL UNIQUE,
    cliente_id              BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    equipamento_id          BIGINT REFERENCES equipamento(id) ON DELETE CASCADE,
    tipo_ferramenta         VARCHAR(100) DEFAULT '',
    marca                   VARCHAR(100) DEFAULT '',
    modelo                  VARCHAR(100) DEFAULT '',
    codigo_identificacao    VARCHAR(100) DEFAULT '',
    defeito_relatado        TEXT DEFAULT '',
    status                  VARCHAR(20) NOT NULL DEFAULT 'orcamento',
    prioridade              VARCHAR(10) NOT NULL DEFAULT 'media',
    descricao_servico       TEXT DEFAULT '',
    solucao_aplicada        TEXT DEFAULT '',
    mao_obra                NUMERIC(10,2) DEFAULT 0,
    taxa_analise            NUMERIC(10,2) DEFAULT 0,
    desconto                NUMERIC(10,2) DEFAULT 0,
    observacoes             TEXT DEFAULT '',
    data_abertura           TIMESTAMP NOT NULL DEFAULT NOW(),
    data_prevista           TIMESTAMP,
    data_aprovacao          TIMESTAMP,
    data_conclusao          TIMESTAMP,
    data_saida              TIMESTAMP,
    data_entrega            TIMESTAMP,
    data_faturamento        TIMESTAMP,
    pago_50_percent         BOOLEAN DEFAULT FALSE,
    data_pagamento_50       TIMESTAMP,
    lancamento_faturamento  INTEGER,
    data_faturamento_total  TIMESTAMP,
    data_retirada           TIMESTAMP,
    forma_pagamento         VARCHAR(20) DEFAULT '',
    pagamento_prazo         BOOLEAN DEFAULT FALSE,
    data_vencimento_prazo   DATE,
    valor_restante_prazo    NUMERIC(10,2) DEFAULT 0,
    tecnico_responsavel_id  BIGINT REFERENCES usuario(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_os_empresa ON ordem_servico(empresa_id);
CREATE INDEX IF NOT EXISTS idx_os_status ON ordem_servico(status);
CREATE INDEX IF NOT EXISTS idx_os_data_abertura ON ordem_servico(data_abertura);
CREATE INDEX IF NOT EXISTS idx_os_numero ON ordem_servico(numero);
CREATE INDEX IF NOT EXISTS idx_os_cliente ON ordem_servico(cliente_id);

-- Item da OS
CREATE TABLE IF NOT EXISTS item_ordem_servico (
    id                BIGSERIAL PRIMARY KEY,
    ordem_servico_id  BIGINT NOT NULL REFERENCES ordem_servico(id) ON DELETE CASCADE,
    produto_id        BIGINT REFERENCES produto(id) ON DELETE SET NULL,
    quantidade        INTEGER NOT NULL DEFAULT 1,
    preco_unitario    NUMERIC(10,2) NOT NULL,
    aguardando_estoque BOOLEAN DEFAULT FALSE,
    status_compra     VARCHAR(15) DEFAULT 'pendente'
);
CREATE INDEX IF NOT EXISTS idx_item_os ON item_ordem_servico(ordem_servico_id);

-- Movimentação de Estoque
CREATE TABLE IF NOT EXISTS movimentacao_estoque (
    id               BIGSERIAL PRIMARY KEY,
    produto_id       BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    tipo             VARCHAR(10) NOT NULL,
    quantidade       INTEGER NOT NULL,
    ordem_servico_id BIGINT REFERENCES ordem_servico(id) ON DELETE SET NULL,
    observacao       TEXT DEFAULT '',
    usuario_id       BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    data             TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Lançamento Financeiro
CREATE TABLE IF NOT EXISTS lancamento_financeiro (
    id               BIGSERIAL PRIMARY KEY,
    empresa_id       BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    descricao        VARCHAR(300) NOT NULL,
    tipo             VARCHAR(10) NOT NULL,
    categoria        VARCHAR(20) NOT NULL DEFAULT 'outros',
    valor            NUMERIC(10,2) NOT NULL,
    data_vencimento  DATE NOT NULL,
    data_pagamento   DATE,
    forma_pagamento  VARCHAR(20) DEFAULT '',
    status           VARCHAR(15) NOT NULL DEFAULT 'pendente',
    ordem_servico_id BIGINT REFERENCES ordem_servico(id) ON DELETE SET NULL,
    cliente_id       BIGINT REFERENCES cliente(id) ON DELETE SET NULL,
    observacoes      TEXT DEFAULT '',
    usuario_id       BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    data_cadastro    TIMESTAMP NOT NULL DEFAULT NOW(),
    parcelado        BOOLEAN DEFAULT FALSE,
    numero_parcelas  INTEGER,
    numero_parcela   INTEGER,
    lancamento_pai_id BIGINT REFERENCES lancamento_financeiro(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_lanc_empresa ON lancamento_financeiro(empresa_id);
CREATE INDEX IF NOT EXISTS idx_lanc_tipo ON lancamento_financeiro(tipo);
CREATE INDEX IF NOT EXISTS idx_lanc_status ON lancamento_financeiro(status);
CREATE INDEX IF NOT EXISTS idx_lanc_vencimento ON lancamento_financeiro(data_vencimento);

-- Abertura de Caixa
CREATE TABLE IF NOT EXISTS abertura_caixa (
    id                    BIGSERIAL PRIMARY KEY,
    empresa_id            BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    data_abertura         TIMESTAMP NOT NULL DEFAULT NOW(),
    data_fechamento       TIMESTAMP,
    saldo_abertura        NUMERIC(10,2) DEFAULT 0,
    saldo_fechamento      NUMERIC(10,2),
    usuario_abertura_id   BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    usuario_fechamento_id BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    observacoes           TEXT DEFAULT ''
);

-- Histórico WhatsApp
CREATE TABLE IF NOT EXISTS historico_whatsapp (
    id               BIGSERIAL PRIMARY KEY,
    cliente_id       BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    telefone         VARCHAR(20) NOT NULL,
    mensagem         TEXT NOT NULL,
    tipo             VARCHAR(10) NOT NULL,
    status           VARCHAR(20) DEFAULT 'enviada',
    ordem_servico_id BIGINT REFERENCES ordem_servico(id) ON DELETE SET NULL,
    data_envio       TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Configuração WhatsApp
CREATE TABLE IF NOT EXISTS configuracao_whatsapp (
    id         BIGSERIAL PRIMARY KEY,
    empresa_id BIGINT UNIQUE REFERENCES empresa(id) ON DELETE CASCADE,
    ativo      BOOLEAN NOT NULL DEFAULT FALSE
);

-- Auditoria
CREATE TABLE IF NOT EXISTS auditoria_log (
    id          BIGSERIAL PRIMARY KEY,
    empresa_id  BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    usuario_id  BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    acao        VARCHAR(20) NOT NULL,
    modelo      VARCHAR(50) DEFAULT '',
    objeto_id   VARCHAR(50) DEFAULT '',
    descricao   TEXT NOT NULL,
    dados_extras JSONB,
    ip          VARCHAR(50) DEFAULT '',
    data        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_audit_empresa_data ON auditoria_log(empresa_id, data);
CREATE INDEX IF NOT EXISTS idx_audit_modelo_acao ON auditoria_log(modelo, acao);

-- Configuração Fiscal
CREATE TABLE IF NOT EXISTS configuracao_fiscal (
    id                   BIGSERIAL PRIMARY KEY,
    empresa_id           BIGINT UNIQUE REFERENCES empresa(id) ON DELETE CASCADE,
    token                VARCHAR(300) DEFAULT '',
    ambiente             VARCHAR(1) DEFAULT '2',
    cnpj_emitente        VARCHAR(18) DEFAULT '',
    razao_social         VARCHAR(200) DEFAULT '',
    nome_fantasia        VARCHAR(200) DEFAULT '',
    inscricao_estadual   VARCHAR(30) DEFAULT '',
    inscricao_municipal  VARCHAR(30) DEFAULT '',
    regime_tributario    VARCHAR(1) DEFAULT '1',
    csc_id               VARCHAR(10) DEFAULT '',
    csc_token            VARCHAR(100) DEFAULT '',
    ativo                BOOLEAN NOT NULL DEFAULT FALSE
);

-- Nota Fiscal
CREATE TABLE IF NOT EXISTS nota_fiscal (
    id              BIGSERIAL PRIMARY KEY,
    empresa_id      BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    ordem_servico_id BIGINT REFERENCES ordem_servico(id) ON DELETE SET NULL,
    tipo            VARCHAR(5) DEFAULT 'nfce',
    status          VARCHAR(20) DEFAULT 'rascunho',
    numero          VARCHAR(20) DEFAULT '',
    serie           VARCHAR(5) DEFAULT '1',
    chave           VARCHAR(50) DEFAULT '',
    numero_ref      VARCHAR(100) DEFAULT '',
    valor_total     NUMERIC(10,2) DEFAULT 0,
    descricao       TEXT DEFAULT '',
    resposta_focus  JSONB,
    caminho_xml     VARCHAR(500) DEFAULT '',
    caminho_danfe   VARCHAR(500) DEFAULT '',
    mensagem_erro   TEXT DEFAULT '',
    data_emissao    TIMESTAMP NOT NULL DEFAULT NOW(),
    data_autorizacao TIMESTAMP
);

-- Tributo Federal
CREATE TABLE IF NOT EXISTS tributo_federal (
    id                         BIGSERIAL PRIMARY KEY,
    empresa_id                 BIGINT NOT NULL REFERENCES empresa(id) ON DELETE CASCADE,
    categoria                  VARCHAR(20) NOT NULL,
    competencia                VARCHAR(30) DEFAULT '',
    valor                      NUMERIC(10,2) NOT NULL,
    data_vencimento            DATE NOT NULL,
    data_pagamento_programada  DATE,
    data_pagamento_realizado   DATE,
    boleto_path                VARCHAR(500),
    comprovante_path           VARCHAR(500),
    observacoes                TEXT DEFAULT '',
    usuario_id                 BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    data_cadastro              TIMESTAMP NOT NULL DEFAULT NOW(),
    lancamento_financeiro_id   BIGINT REFERENCES lancamento_financeiro(id) ON DELETE SET NULL
);

-- Backup
CREATE TABLE IF NOT EXISTS backup_registro (
    id            BIGSERIAL PRIMARY KEY,
    empresa_id    BIGINT REFERENCES empresa(id) ON DELETE CASCADE,
    arquivo_nome  VARCHAR(255) NOT NULL,
    arquivo_path  VARCHAR(500) NOT NULL,
    tamanho_bytes BIGINT DEFAULT 0,
    tipo          VARCHAR(20) DEFAULT 'manual',
    status        VARCHAR(15) DEFAULT 'sucesso',
    observacoes   TEXT DEFAULT '',
    usuario_id    BIGINT REFERENCES usuario(id) ON DELETE SET NULL,
    data_criacao  TIMESTAMP NOT NULL DEFAULT NOW()
);
