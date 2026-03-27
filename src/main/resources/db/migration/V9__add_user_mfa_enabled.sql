alter table users
    add column mfa_enabled boolean not null default false;
