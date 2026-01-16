--Schema generated based on existing DB by datagrip

create table execution_record
(
    dataset_id     varchar(20)  not null,
    execution_id   varchar(50)  not null,
    execution_name varchar(50),
    record_id      varchar(300) not null,
    record_data    text,
    primary key (dataset_id, execution_id, record_id)
);

alter table execution_record
    owner to admin;

create index execution_record_dataset_id_execution_id_idx
    on execution_record (dataset_id, execution_id);

create table execution_record_exception_log
(
    dataset_id     varchar(20)  not null,
    execution_id   varchar(50)  not null,
    execution_name varchar(50),
    record_id      varchar(300) not null,
    exception      text,
    primary key (dataset_id, execution_id, record_id)
);

alter table execution_record_exception_log
    owner to admin;

create index execution_record_exception_log_dataset_id_execution_id_idx
    on execution_record_exception_log (dataset_id, execution_id);

create table execution_record_external_identifier
(
    is_deleted   boolean      not null,
    dataset_id   varchar(20)  not null,
    execution_id varchar(50)  not null,
    record_id    varchar(300) not null,
    record_index integer,
    datestamp    timestamp,
    primary key (dataset_id, execution_id, record_id)
);

alter table execution_record_external_identifier
    owner to admin;

create unique index execution_record_external_identifier_dataset_id_execution_id_re
    on execution_record_external_identifier (dataset_id, execution_id, record_index);

create table task_info
(
    commit_count bigint not null,
    end_time     timestamp(6),
    start_time   timestamp(6),
    task_id      bigint not null
        primary key,
    write_count  bigint not null,
    task_name    varchar(255)
);

alter table task_info
    owner to admin;

create table task_info_1
(
    task_id      bigint not null
        primary key,
    commit_count bigint,
    write_count  bigint
);

alter table task_info_1
    owner to admin;

