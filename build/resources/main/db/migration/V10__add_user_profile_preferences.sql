alter table users
    add column preferred_language varchar(35) null;

alter table users
    add column time_zone varchar(64) null;
