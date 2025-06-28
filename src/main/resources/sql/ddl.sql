--create database learn;
--drop table db_spec.visit
create schema if not exists db_spec;

create table if not exists db_spec.visit (
    id serial not null primary key,
    fio varchar(255) not null,
    contact varchar(255) not null,
    start_time timestamp not null,
    end_time timestamp not null
);

comment on table db_spec.visit is 'Предварительная запись';
comment on column db_spec.visit.id is 'Уникальный идентификатор строки';
comment on column db_spec.visit.fio is 'ФИО записавшегося';
comment on column db_spec.visit.contact is 'Контакт для связи (номер телефона, email, имя аккаунта в Telegram/WhatsApp)';
comment on column db_spec.visit.start_time is 'Дата и время начала сеанса';
comment on column db_spec.visit.end_time is 'Дата и время окончания сеанса';

insert into db_spec.visit(fio, contact, start_time, end_time)
values('Смирнов Сергей Сергеевич', '+79998887766', current_date + interval '1' day + interval '9' hour, current_date + interval '1' day + interval '10' hour);

insert into db_spec.visit(fio, contact, start_time, end_time)
values('Кузнецов Николай Николаевич', '+79031408124', current_date + interval '1' day + interval '10' hour, current_date + interval '1' day + interval '11' hour);

insert into db_spec.visit(fio, contact, start_time, end_time)
values('Фёдоров Виктор Викторович', '+79030120011', current_date + interval '1' day + interval '11' hour, current_date + interval '1' day + interval '12' hour);





WITH RECURSIVE cte AS (
 -- Начальное значение
 SELECT 1 AS id,
 'Фёдоров Виктор Викторович' || '1' AS fio,
 '+79030120011'::bigint AS contact,
 current_date + interval '1' day + interval '11' hour AS start_time,
 current_date + interval '1' day + interval '12' hour AS end_time
 UNION ALL
 -- Рекурсивная часть
 SELECT id + 1,
 'Фёдоров Виктор Викторович' || (id + 1)::text,
 (contact + id)::bigint,
 start_time + interval '1' hour,
 end_time + interval '1' hour
 FROM cte
 WHERE id < 10000
)
INSERT INTO db_spec.visit (fio, contact, start_time, end_time)
SELECT 
 fio,
 ('+' || contact::text) AS contact,
 start_time,
 end_time
FROM cte;

select * from db_spec.visit v where id = 5 
delete from db_spec.visit where id = 1000


SELECT 
    pid,
    usename,
    query,
    state,
    --waiting,
    query_start,
    now() - query_start as duration
FROM pg_stat_activity
WHERE state <> 'idle';


SELECT 
    locktype, 
    mode, 
    GRANTED, 
    pg_locks.pid, 
    query 
FROM pg_locks 
JOIN pg_stat_activity ON pg_locks.pid = pg_stat_activity.pid;



SHOW max_connections;
SHOW shared_buffers;
SHOW work_mem;
SHOW effective_cache_size;

SELECT 
    relname,
    --n_live_tuples,
    --n_dead_tuples,
    last_vacuum,
    last_autovacuum
FROM pg_stat_user_tables;


SHOW max_connections;
SHOW max_prepared_transactions;
SHOW superuser_reserved_connections;

set max_connections = 150
set shared_buffers = 512
