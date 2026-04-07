CREATE TABLE program_curriculum_items (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    academic_year INT NOT NULL,
    semester_number INT NOT NULL,
    display_order INT NOT NULL,
    required BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_program_curriculum_item UNIQUE (program_id, subject_id, academic_year, semester_number)
);

CREATE INDEX idx_program_curriculum_program_term
    ON program_curriculum_items(program_id, academic_year, semester_number, display_order);

CREATE TABLE planned_registrations (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    semester_id BIGINT NOT NULL REFERENCES semesters(id) ON DELETE CASCADE,
    subject_offering_id BIGINT NOT NULL REFERENCES subject_offerings(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_planned_registration_student_offering UNIQUE (student_id, subject_offering_id)
);

CREATE INDEX idx_planned_registration_student_semester
    ON planned_registrations(student_id, semester_id);
