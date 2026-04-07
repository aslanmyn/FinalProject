-- Prevent more than one active dorm application per student
create unique index if not exists uq_dorm_applications_active_student
    on dorm_applications (student_id)
    where status in ('DRAFT', 'SUBMITTED', 'APPROVED');

-- Prevent overlapping active laundry bookings for the same machine
create extension if not exists btree_gist;

alter table laundry_bookings
    drop constraint if exists ex_laundry_machine_timeslot_active;

alter table laundry_bookings
    add constraint ex_laundry_machine_timeslot_active
        exclude using gist (
            machine_id with =,
            tstzrange(time_slot_start, time_slot_end, '[)') with &&
        )
        where (status in ('BOOKED', 'IN_PROGRESS'));
