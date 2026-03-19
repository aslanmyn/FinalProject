alter table attendance_sessions
    add column if not exists attendance_close_at timestamp(6) with time zone,
    add column if not exists opened_at timestamp(6) with time zone,
    add column if not exists closed_at timestamp(6) with time zone,
    add column if not exists allow_teacher_override boolean not null default true,
    add column if not exists check_in_code varchar(32),
    add column if not exists check_in_mode varchar(32) not null default 'ONE_CLICK',
    add column if not exists status varchar(32) not null default 'CLOSED';

alter table attendance_sessions
    drop constraint if exists attendance_sessions_check;

alter table attendance_sessions
    add constraint attendance_sessions_status_check
        check (status in ('DRAFT', 'OPEN', 'CLOSED'));

alter table attendance_sessions
    add constraint attendance_sessions_check_in_mode_check
        check (check_in_mode in ('ONE_CLICK', 'CODE'));

update attendance_sessions
set status = case when locked then 'CLOSED' else 'CLOSED' end,
    opened_at = coalesce(opened_at, created_at),
    closed_at = case when locked then coalesce(closed_at, created_at) else closed_at end
where status is null or opened_at is null or (locked and closed_at is null);

alter table attendances
    add column if not exists marked_at timestamp(6) with time zone,
    add column if not exists updated_at timestamp(6) with time zone,
    add column if not exists teacher_confirmed boolean not null default true,
    add column if not exists marked_by varchar(32) not null default 'TEACHER';

alter table attendances
    drop constraint if exists attendances_marked_by_check;

alter table attendances
    add constraint attendances_marked_by_check
        check (marked_by in ('STUDENT', 'TEACHER', 'SYSTEM'));

update attendances
set marked_at = coalesce(marked_at, now()),
    updated_at = coalesce(updated_at, now())
where marked_at is null or updated_at is null;
