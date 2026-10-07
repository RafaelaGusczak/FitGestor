-- FitGestor — esquema do banco (PostgreSQL / Supabase)
-- Rode este arquivo no SQL Editor do Supabase (ou com psql). É seguro rodar mais de uma vez.

create schema if not exists extensions;
create extension if not exists pgcrypto with schema extensions;

create table if not exists usuario (
    id          serial primary key,
    login       text not null unique,
    nome        text not null,
    email       text,
    senha_hash  text not null
);

create table if not exists aluno (
    id          serial primary key,
    nome        text not null,
    cpf         text not null unique,
    nascimento  date,
    telefone    text,
    email       text,
    situacao    text not null default 'Ativo' check (situacao in ('Ativo', 'Inativo'))
);

create table if not exists plano (
    id              serial primary key,
    nome            text not null unique,
    descricao       text,
    valor_mensal    numeric(10,2) not null check (valor_mensal >= 0),
    duracao_meses   int not null check (duracao_meses > 0)
);

create table if not exists instrutor (
    id              serial primary key,
    nome            text not null,
    cpf             text not null unique,
    especialidade   text,
    telefone        text
);

create table if not exists aula (
    id              serial primary key,
    modalidade      text not null,
    dia_semana      text not null check (dia_semana in ('Segunda','Terça','Quarta','Quinta','Sexta','Sábado','Domingo')),
    horario         time not null,
    instrutor_id    int references instrutor(id) on delete restrict,
    vagas           int not null check (vagas > 0)
);

create table if not exists matricula (
    id              serial primary key,
    aluno_id        int not null references aluno(id) on delete restrict,
    plano_id        int not null references plano(id) on delete restrict,
    data_inicio     date not null,
    data_termino    date not null,
    valor_mensal    numeric(10,2) not null check (valor_mensal >= 0),
    status          text not null default 'Ativa' check (status in ('Ativa', 'Trancada', 'Cancelada')),
    check (data_termino > data_inicio)
);

create table if not exists pagamento (
    id              serial primary key,
    matricula_id    int not null references matricula(id) on delete cascade,
    vencimento      date not null,
    valor           numeric(10,2) not null check (valor >= 0),
    forma           text check (forma in ('PIX', 'Dinheiro', 'Cartão de crédito', 'Cartão de débito', 'Boleto')),
    data_pagamento  date,
    -- uma mensalidade paga precisa ter forma de pagamento
    check (data_pagamento is null or forma is not null)
);

create table if not exists checkin (
    id          serial primary key,
    aluno_id    int not null references aluno(id) on delete cascade,
    aula_id     int not null references aula(id) on delete cascade,
    data_hora   timestamptz not null default now()
);

create index if not exists idx_matricula_aluno     on matricula(aluno_id);
create index if not exists idx_matricula_termino   on matricula(data_termino);
create index if not exists idx_pagamento_matricula on pagamento(matricula_id);
create index if not exists idx_pagamento_venc      on pagamento(vencimento);
create index if not exists idx_checkin_aula_data   on checkin(aula_id, data_hora);
create index if not exists idx_checkin_aluno_data  on checkin(aluno_id, data_hora);

-- Segurança: o Supabase expõe as tabelas do schema "public" numa API REST pública.
-- Com RLS ligado e nenhuma policy, essa API não enxerga nada. O app desktop conecta
-- direto no Postgres (JDBC) com o usuário "postgres", que ignora o RLS e continua funcionando.
alter table usuario    enable row level security;
alter table aluno      enable row level security;
alter table plano      enable row level security;
alter table instrutor  enable row level security;
alter table aula       enable row level security;
alter table matricula  enable row level security;
alter table pagamento  enable row level security;
alter table checkin    enable row level security;
