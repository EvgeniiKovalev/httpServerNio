create database learn;

drop table db_spec.appointments
create schema if not exists db_spec;

create table if not exists db_spec.appointments (
    id serial not null primary key,
    fio varchar(255) not null,
    contact varchar(255) not null,
    start_time timestamp not null,
    end_time timestamp not null
);

comment on table db_spec.appointments is 'Предварительная запись';
comment on column db_spec.appointments.id is 'Уникальный идентификатор специалиста';
comment on column db_spec.appointments.fio is 'ФИО записавшегося';
comment on column db_spec.appointments.contact is 'Контакт для связи (номер телефона, email, имя аккаунта в Telegram/WhatsApp)';
comment on column db_spec.appointments.start_time is 'Дата и время начала сеанса';
comment on column db_spec.appointments.end_time is 'Дата и время окончания сеанса';

insert into db_spec.appointments(fio, contact, start_time, end_time)
values('Смирнов Сергей Сергеевич', '+79998887766', current_date + interval '9' hour, current_date + interval '10' hour);

insert into db_spec.appointments(fio, contact, start_time, end_time)
values('Кузнецов Николай Николаевич', '+79031408124', current_date + interval '10' hour, current_date + interval '11' hour);

insert into db_spec.appointments(fio, contact, start_time, end_time)
values('Фёдоров Виктор Викторович', '+79030120011', current_date + interval '11' hour, current_date + interval '12' hour);
