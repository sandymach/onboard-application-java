create table if not exists application (
    application_id char(36) primary key,
    country varchar(32) not null,
    customer_type varchar(64) not null,
    current_step_code varchar(128),
    status varchar(64) not null,
    scenario_key varchar(128),
    resume_token_hash varchar(256),
    resume_token_expires_at datetime(6),
    expires_at datetime(6),
    decision_reason varchar(256),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    submitted_at datetime(6)
);

create unique index ux_application_resume_token_hash
    on application (resume_token_hash);

create table if not exists application_events (
    application_event_id char(36) primary key,
    application_id char(36) not null,
    event_type varchar(128) not null,
    result_code varchar(256),
    request_id varchar(128),
    occurred_at datetime(6) not null,
    index ix_application_events_application_time (application_id, occurred_at),
    constraint fk_application_events_application foreign key (application_id) references application (application_id)
);

create table if not exists applicant (
    applicant_id char(10) primary key,
    application_id char(36) not null,
    applicant_type varchar(64),
    country varchar(32),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    index ix_applicant_application (application_id),
    constraint fk_applicant_application foreign key (application_id) references application (application_id)
);

create table if not exists customer (
    customer_id char(10) primary key,
    application_id char(36) not null,
    customer_type varchar(64),
    country varchar(32),
    status varchar(64),
    created_at datetime(6) not null,
    updated_at datetime(6) not null,
    index ix_customer_application (application_id),
    constraint fk_customer_application foreign key (application_id) references application (application_id)
);

create table if not exists decision (
    decision_id char(36) primary key,
    application_id char(36) not null,
    decision_status varchar(64) not null,
    reason_code varchar(256),
    decision_source varchar(64),
    decided_by varchar(128),
    request_id varchar(128),
    decided_at datetime(6) not null,
    index ix_decision_application_time (application_id, decided_at),
    constraint fk_decision_application foreign key (application_id) references application (application_id)
);

create table if not exists idv (
    idv_id char(36) primary key,
    application_id char(36) not null,
    applicant_id char(10),
    outcome varchar(64) not null,
    provider varchar(128),
    reason_code varchar(128),
    message varchar(512),
    request_id varchar(128),
    checked_at datetime(6) not null,
    index ix_idv_application_time (application_id, checked_at),
    constraint fk_idv_application foreign key (application_id) references application (application_id),
    constraint fk_idv_applicant foreign key (applicant_id) references applicant (applicant_id)
);

create table if not exists agreement (
    agreement_id char(36) primary key,
    application_id char(36) not null,
    agreement_reference varchar(256),
    outcome varchar(64) not null,
    reason_code varchar(128),
    message varchar(512),
    created_at datetime(6) not null,
    index ix_agreement_application_time (application_id, created_at),
    constraint fk_agreement_application foreign key (application_id) references application (application_id)
);

create table if not exists signing (
    signing_id char(36) primary key,
    application_id char(36) not null,
    signing_reference varchar(256),
    signing_mode varchar(64),
    outcome varchar(64) not null,
    reason_code varchar(128),
    message varchar(512),
    signed_at datetime(6) not null,
    index ix_signing_application_time (application_id, signed_at),
    constraint fk_signing_application foreign key (application_id) references application (application_id)
);
