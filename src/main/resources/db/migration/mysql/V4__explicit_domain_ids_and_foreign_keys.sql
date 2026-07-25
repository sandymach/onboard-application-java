-- Converts earlier tables that used generic `id` primary keys to explicit domain IDs.
-- Also adds application foreign keys to diagnostic tables.

drop procedure if exists rename_column_if_exists;
drop procedure if exists add_fk_if_absent;
drop procedure if exists drop_fk_if_exists;

delimiter //

create procedure rename_column_if_exists(
    in target_table varchar(128),
    in old_column varchar(128),
    in new_column varchar(128),
    in column_definition varchar(256)
)
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = target_table
          and column_name = old_column
    ) and not exists (
        select 1
        from information_schema.columns
        where table_schema = database()
          and table_name = target_table
          and column_name = new_column
    ) then
        set @sql = concat('alter table `', target_table, '` change `', old_column, '` `', new_column, '` ', column_definition);
        prepare stmt from @sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end//

create procedure drop_fk_if_exists(in target_table varchar(128), in fk_name varchar(128))
begin
    if exists (
        select 1
        from information_schema.table_constraints
        where constraint_schema = database()
          and table_name = target_table
          and constraint_name = fk_name
          and constraint_type = 'FOREIGN KEY'
    ) then
        set @sql = concat('alter table `', target_table, '` drop foreign key `', fk_name, '`');
        prepare stmt from @sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end//

create procedure add_fk_if_absent(
    in target_table varchar(128),
    in fk_name varchar(128),
    in source_column varchar(128),
    in referenced_table varchar(128),
    in referenced_column varchar(128)
)
begin
    if exists (
        select 1
        from information_schema.tables
        where table_schema = database()
          and table_name = target_table
    ) and not exists (
        select 1
        from information_schema.table_constraints
        where constraint_schema = database()
          and table_name = target_table
          and constraint_name = fk_name
          and constraint_type = 'FOREIGN KEY'
    ) then
        set @sql = concat(
            'alter table `', target_table, '` add constraint `', fk_name, '` foreign key (`',
            source_column, '`) references `', referenced_table, '` (`', referenced_column, '`)'
        );
        prepare stmt from @sql;
        execute stmt;
        deallocate prepare stmt;
    end if;
end//

delimiter ;

call drop_fk_if_exists('application_events', 'fk_application_events_application');
call drop_fk_if_exists('applicant', 'fk_applicant_application');
call drop_fk_if_exists('customer', 'fk_customer_application');
call drop_fk_if_exists('decision', 'fk_decision_application');
call drop_fk_if_exists('idv', 'fk_idv_application');
call drop_fk_if_exists('idv', 'fk_idv_applicant');
call drop_fk_if_exists('agreement', 'fk_agreement_application');
call drop_fk_if_exists('signing', 'fk_signing_application');

call rename_column_if_exists('application', 'id', 'application_id', 'char(36) not null');
call rename_column_if_exists('application_events', 'id', 'application_event_id', 'char(36) not null');
call rename_column_if_exists('applicant', 'id', 'applicant_id', 'char(10) not null');
call rename_column_if_exists('customer', 'id', 'customer_id', 'char(10) not null');
call rename_column_if_exists('decision', 'id', 'decision_id', 'char(36) not null');
call rename_column_if_exists('idv', 'id', 'idv_id', 'char(36) not null');
call rename_column_if_exists('agreement', 'id', 'agreement_id', 'char(36) not null');
call rename_column_if_exists('signing', 'id', 'signing_id', 'char(36) not null');
call rename_column_if_exists('step_result', 'id', 'step_result_id', 'char(36) not null');
call rename_column_if_exists('integration_result', 'id', 'integration_result_id', 'char(36) not null');

call add_fk_if_absent('application_events', 'fk_application_events_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('applicant', 'fk_applicant_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('customer', 'fk_customer_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('decision', 'fk_decision_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('idv', 'fk_idv_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('idv', 'fk_idv_applicant', 'applicant_id', 'applicant', 'applicant_id');
call add_fk_if_absent('agreement', 'fk_agreement_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('signing', 'fk_signing_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('step_result', 'fk_step_result_application', 'application_id', 'application', 'application_id');
call add_fk_if_absent('integration_result', 'fk_integration_result_application', 'application_id', 'application', 'application_id');

drop procedure if exists rename_column_if_exists;
drop procedure if exists add_fk_if_absent;
drop procedure if exists drop_fk_if_exists;
