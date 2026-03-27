alter table audit_events
    add column branch_id uuid null;

alter table audit_events
    add column outcome varchar(32) not null default 'SUCCESS';

alter table audit_events
    add constraint fk_audit_events_branch
        foreign key (branch_id) references branches (id);

alter table audit_events
    add constraint chk_audit_events_outcome
        check (outcome in ('SUCCESS', 'FAILURE'));

create index idx_audit_events_branch_id
    on audit_events (branch_id);

create index idx_audit_events_outcome
    on audit_events (outcome);

create index idx_audit_events_occurred_at
    on audit_events (occurred_at);
