ALTER TABLE planned_registrations
    ADD COLUMN subject_id BIGINT;

UPDATE planned_registrations pr
SET subject_id = so.subject_id
FROM subject_offerings so
WHERE pr.subject_offering_id = so.id
  AND pr.subject_id IS NULL;

ALTER TABLE planned_registrations
    ALTER COLUMN subject_id SET NOT NULL;

ALTER TABLE planned_registrations
    ADD CONSTRAINT fk_planned_registration_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE;

ALTER TABLE planned_registrations
    ALTER COLUMN subject_offering_id DROP NOT NULL;

ALTER TABLE planned_registrations
    DROP CONSTRAINT uq_planned_registration_student_offering;

ALTER TABLE planned_registrations
    ADD CONSTRAINT uq_planned_registration_student_semester_subject
        UNIQUE (student_id, semester_id, subject_id);

CREATE INDEX idx_planned_registration_student_semester_subject
    ON planned_registrations(student_id, semester_id, subject_id);
