create table if not exists step_result (
    id char(36) primary key,
    application_id char(36) not null,
    step_code varchar(128) not null,
    answers_json text,
    answers_fingerprint varchar(256),
    completed_at datetime(6) not null,
    constraint ux_step_result_application_step unique (application_id, step_code)
);

create table if not exists integration_result (
    id char(36) primary key,
    application_id char(36) not null,
    integration_type varchar(128) not null,
    outcome varchar(64) not null,
    reason_code varchar(128),
    message varchar(512),
    answers_fingerprint varchar(512),
    request_id varchar(128),
    checked_at datetime(6) not null
);
