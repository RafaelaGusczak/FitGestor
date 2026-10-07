-- FitGestor — dados iniciais. Rode depois do schema.sql, uma única vez.
-- Usuário de acesso: admin / admin123  (TROQUE a senha depois do primeiro acesso!)

insert into usuario (login, nome, email, senha_hash)
select 'admin', 'Recepção', 'admin@fitgestor.com', extensions.crypt('admin123', extensions.gen_salt('bf'))
where not exists (select 1 from usuario where login = 'admin');

insert into plano (nome, descricao, valor_mensal, duracao_meses) values
  ('Mensal',     'Acesso livre à musculação',     99.90, 1),
  ('Trimestral', 'Musculação + aulas coletivas',  89.90, 3),
  ('Semestral',  'Musculação + aulas coletivas',  79.90, 6),
  ('Anual',      'Acesso completo + avaliação',   69.90, 12)
on conflict (nome) do nothing;

insert into instrutor (nome, cpf, especialidade, telefone) values
  ('Ricardo Menezes', '111.222.333-44', 'Musculação',     '(47) 99111-0001'),
  ('Juliana Prado',   '222.333.444-55', 'Spinning',       '(47) 99222-0002'),
  ('Bruno Teixeira',  '333.444.555-66', 'Funcional',      '(47) 99333-0003'),
  ('Camila Rocha',    '444.555.666-77', 'Yoga / Pilates', '(47) 99444-0004')
on conflict (cpf) do nothing;

insert into aluno (nome, cpf, nascimento, telefone, email, situacao) values
  ('Mariana Souza',       '123.456.789-00', '1995-03-14', '(47) 99912-3456', 'mariana.souza@email.com', 'Ativo'),
  ('Carlos Eduardo Lima', '987.654.321-00', '1990-07-02', '(47) 98801-2233', null, 'Ativo'),
  ('Fernanda Oliveira',   '456.789.123-00', '1988-11-23', '(47) 99655-7788', null, 'Ativo'),
  ('João Pedro Martins',  '321.654.987-00', '2000-01-30', '(47) 99702-4411', null, 'Inativo'),
  ('Ana Beatriz Costa',   '654.987.321-00', '1998-05-09', '(47) 98433-9012', null, 'Ativo'),
  ('Rafael Almeida',      '159.357.486-00', '1993-09-17', '(47) 99188-6677', null, 'Ativo')
on conflict (cpf) do nothing;

insert into aula (modalidade, dia_semana, horario, instrutor_id, vagas)
select m, d, h::time, (select id from instrutor where nome = i), v
from (values
  ('Spinning',  'Segunda', '07:00', 'Juliana Prado',  20),
  ('Funcional', 'Terça',   '18:30', 'Bruno Teixeira', 15),
  ('Yoga',      'Quarta',  '19:30', 'Camila Rocha',   12),
  ('Spinning',  'Quinta',  '07:00', 'Juliana Prado',  20),
  ('Funcional', 'Sexta',   '18:30', 'Bruno Teixeira', 15),
  ('Pilates',   'Sábado',  '09:00', 'Camila Rocha',   10)
) as t(m, d, h, i, v)
where not exists (select 1 from aula);

-- Matrículas e mensalidades de exemplo (datas relativas a hoje, para os relatórios terem o que mostrar)
do $$
begin
  if not exists (select 1 from matricula) then
    insert into matricula (aluno_id, plano_id, data_inicio, data_termino, valor_mensal, status)
    select a.id, p.id, current_date - off, current_date - off + (p.duracao_meses || ' months')::interval, p.valor_mensal, 'Ativa'
    from (values ('Mariana Souza','Trimestral',10), ('Carlos Eduardo Lima','Anual',60),
                 ('Fernanda Oliveira','Semestral',30), ('João Pedro Martins','Mensal',40),
                 ('Rafael Almeida','Mensal',20)) as v(aluno, plano, off)
    join aluno a on a.nome = v.aluno join plano p on p.nome = v.plano;

    -- uma mensalidade por mês de duração, a partir do início
    insert into pagamento (matricula_id, vencimento, valor)
    select m.id, (m.data_inicio + (g || ' months')::interval)::date, m.valor_mensal
    from matricula m
    join plano p on p.id = m.plano_id
    cross join lateral generate_series(0, p.duracao_meses - 1) g;

    -- as já vencidas dos alunos ativos (exceto João Pedro) foram pagas
    update pagamento set forma = 'PIX', data_pagamento = vencimento
    where vencimento <= current_date
      and matricula_id not in (select m.id from matricula m join aluno a on a.id = m.aluno_id
                               where a.nome = 'João Pedro Martins');
  end if;
end $$;
