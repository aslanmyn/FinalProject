-- Add fourth and fifth next-semester variants for key SITE year 2 subjects
-- so the assistant can satisfy more schedule preferences.

CREATE OR REPLACE FUNCTION ensure_subject_offering_variant(
    p_semester_name TEXT,
    p_subject_code TEXT,
    p_teacher_email TEXT,
    p_day_of_week INTEGER,
    p_start_time TIME,
    p_end_time TIME,
    p_lesson_type TEXT,
    p_room TEXT,
    p_capacity INTEGER
) RETURNS BIGINT AS $$
DECLARE
    v_semester_id BIGINT;
    v_subject_id BIGINT;
    v_teacher_id BIGINT;
    v_offering_id BIGINT;
BEGIN
    SELECT id INTO v_semester_id FROM semesters WHERE name = p_semester_name LIMIT 1;
    SELECT id INTO v_subject_id FROM subjects WHERE code = p_subject_code LIMIT 1;
    SELECT id INTO v_teacher_id FROM teachers WHERE email = p_teacher_email LIMIT 1;

    IF v_semester_id IS NULL OR v_subject_id IS NULL OR v_teacher_id IS NULL THEN
        RETURN NULL;
    END IF;

    SELECT so.id
    INTO v_offering_id
    FROM subject_offerings so
    WHERE so.semester_id = v_semester_id
      AND so.subject_id = v_subject_id
      AND so.teacher_id = v_teacher_id
      AND so.day_of_week = p_day_of_week
      AND so.start_time = p_start_time
      AND so.end_time = p_end_time
      AND so.lesson_type = p_lesson_type
      AND so.room = p_room
    LIMIT 1;

    IF v_offering_id IS NULL THEN
        INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
        VALUES (p_capacity, p_day_of_week, p_start_time, p_end_time, v_semester_id, v_subject_id, v_teacher_id, p_lesson_type, p_room)
        RETURNING id INTO v_offering_id;
    END IF;

    RETURN v_offering_id;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION ensure_meeting_time(
    p_offering_id BIGINT,
    p_day_of_week INTEGER,
    p_start_time TIME,
    p_end_time TIME,
    p_lesson_type TEXT,
    p_room TEXT
) RETURNS VOID AS $$
BEGIN
    IF p_offering_id IS NULL THEN
        RETURN;
    END IF;

    INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
    SELECT p_day_of_week, p_end_time, p_start_time, p_offering_id, p_lesson_type, p_room
    WHERE NOT EXISTS (
        SELECT 1
        FROM meeting_times mt
        WHERE mt.subject_offering_id = p_offering_id
          AND mt.day_of_week = p_day_of_week
          AND mt.start_time = p_start_time
          AND mt.end_time = p_end_time
          AND mt.lesson_type = p_lesson_type
          AND mt.room = p_room
    );
END;
$$ LANGUAGE plpgsql;

-- CSCI2104 alternative C
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2104', 'r.serikbayev@kbtu.kz', 1, TIME '14:00', TIME '16:00', 'LECTURE', 'L-427', 20),
    1, TIME '14:00', TIME '16:00', 'LECTURE', 'L-427'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2104', 'r.serikbayev@kbtu.kz', 1, TIME '14:00', TIME '16:00', 'LECTURE', 'L-427', 20),
    3, TIME '16:00', TIME '17:00', 'PRACTICE', 'P-427'
);

-- CSCI2104 alternative D
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2104', 'a.nurgaliyev@kbtu.kz', 0, TIME '08:00', TIME '10:00', 'LECTURE', 'L-427', 20),
    0, TIME '08:00', TIME '10:00', 'LECTURE', 'L-427'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2104', 'a.nurgaliyev@kbtu.kz', 0, TIME '08:00', TIME '10:00', 'LECTURE', 'L-427', 20),
    2, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-427'
);

-- CSCI2105 alternative C
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2105', 'a.nurgaliyev@kbtu.kz', 0, TIME '16:00', TIME '18:00', 'LECTURE', 'L-428', 20),
    0, TIME '16:00', TIME '18:00', 'LECTURE', 'L-428'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2105', 'a.nurgaliyev@kbtu.kz', 0, TIME '16:00', TIME '18:00', 'LECTURE', 'L-428', 20),
    2, TIME '12:00', TIME '13:00', 'PRACTICE', 'P-428'
);

-- CSCI2105 alternative D
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2105', 'r.serikbayev@kbtu.kz', 1, TIME '08:00', TIME '10:00', 'LECTURE', 'L-428', 20),
    1, TIME '08:00', TIME '10:00', 'LECTURE', 'L-428'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'CSCI2105', 'r.serikbayev@kbtu.kz', 1, TIME '08:00', TIME '10:00', 'LECTURE', 'L-428', 20),
    3, TIME '09:00', TIME '10:00', 'PRACTICE', 'P-428'
);

-- INFT2102 alternative C
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2102', 'r.serikbayev@kbtu.kz', 1, TIME '16:00', TIME '18:00', 'LECTURE', 'L-418', 20),
    1, TIME '16:00', TIME '18:00', 'LECTURE', 'L-418'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2102', 'r.serikbayev@kbtu.kz', 1, TIME '16:00', TIME '18:00', 'LECTURE', 'L-418', 20),
    3, TIME '12:00', TIME '13:00', 'PRACTICE', 'P-418'
);

-- INFT2102 alternative D
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2102', 'a.nurgaliyev@kbtu.kz', 2, TIME '08:00', TIME '10:00', 'LECTURE', 'L-418', 20),
    2, TIME '08:00', TIME '10:00', 'LECTURE', 'L-418'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2102', 'a.nurgaliyev@kbtu.kz', 2, TIME '08:00', TIME '10:00', 'LECTURE', 'L-418', 20),
    0, TIME '10:00', TIME '11:00', 'PRACTICE', 'P-418'
);

-- INFT2205 alternative C
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2205', 'a.nurgaliyev@kbtu.kz', 3, TIME '12:00', TIME '14:00', 'LECTURE', 'L-382', 20),
    3, TIME '12:00', TIME '14:00', 'LECTURE', 'L-382'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2205', 'a.nurgaliyev@kbtu.kz', 3, TIME '12:00', TIME '14:00', 'LECTURE', 'L-382', 20),
    1, TIME '16:00', TIME '17:00', 'PRACTICE', 'P-382'
);

-- INFT2205 alternative D
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2205', 'r.serikbayev@kbtu.kz', 0, TIME '09:00', TIME '11:00', 'LECTURE', 'L-382', 20),
    0, TIME '09:00', TIME '11:00', 'LECTURE', 'L-382'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'INFT2205', 'r.serikbayev@kbtu.kz', 0, TIME '09:00', TIME '11:00', 'LECTURE', 'L-382', 20),
    2, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-382'
);

-- HUM1101 alternative C
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 0, TIME '14:00', TIME '15:00', 'PRACTICE', 'P-379', 20),
    0, TIME '14:00', TIME '15:00', 'PRACTICE', 'P-379'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 0, TIME '14:00', TIME '15:00', 'PRACTICE', 'P-379', 20),
    2, TIME '16:00', TIME '17:00', 'PRACTICE', 'P-379'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 0, TIME '14:00', TIME '15:00', 'PRACTICE', 'P-379', 20),
    3, TIME '18:00', TIME '19:00', 'PRACTICE', 'P-379'
);

-- HUM1101 alternative D
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 1, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-379', 20),
    1, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-379'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 1, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-379', 20),
    2, TIME '10:00', TIME '11:00', 'PRACTICE', 'P-379'
);
SELECT ensure_meeting_time(
    ensure_subject_offering_variant('2026-2027 Fall', 'HUM1101', 'a.beketov@kbtu.kz', 1, TIME '08:00', TIME '09:00', 'PRACTICE', 'P-379', 20),
    3, TIME '09:00', TIME '10:00', 'PRACTICE', 'P-379'
);

DROP FUNCTION IF EXISTS ensure_meeting_time(BIGINT, INTEGER, TIME, TIME, TEXT, TEXT);
DROP FUNCTION IF EXISTS ensure_subject_offering_variant(TEXT, TEXT, TEXT, INTEGER, TIME, TIME, TEXT, TEXT, INTEGER);
