create schema "batch-framework";

set search_path to "batch-framework";

create table if not exists task_info
(
    task_id      bigint not null
    primary key,
    commit_count bigint,
    write_count  bigint
);

create table if not exists execution_record
(
    dataset_id     varchar(20)  not null,
    execution_id   varchar(50)  not null,
    execution_name varchar(50),
    record_id      varchar(300) not null,
    record_data    text,
    primary key (dataset_id, execution_id, record_id)
    );

create index execution_record_dataset_id_execution_id_idx
    on execution_record (dataset_id, execution_id);

create table if not exists execution_record_exception_log
(
    dataset_id     varchar(20)  not null,
    execution_id   varchar(50)  not null,
    execution_name varchar(50),
    record_id      varchar(300) not null,
    exception      text,
    primary key (dataset_id, execution_id, record_id)
    );

create index execution_record_exception_log_dataset_id_execution_id_idx
    on execution_record_exception_log (dataset_id, execution_id);

create table if not exists execution_record_external_identifier
(
    is_deleted   boolean      not null,
    dataset_id   varchar(20)  not null,
    execution_id varchar(50)  not null,
    record_id    varchar(300) not null,
    record_index integer,
    datestamp    timestamp,
    primary key (dataset_id, execution_id, record_id)
    );

create unique index execution_record_external_identifier_dataset_id_execution_id_re
    on execution_record_external_identifier (dataset_id, execution_id, record_index);

