ALTER TABLE fx_registrations
    ADD CONSTRAINT uq_fx_registration_student_offering UNIQUE (student_id, subject_offering_id);
