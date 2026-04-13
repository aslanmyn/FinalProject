-- Add extra next-semester section variants for SITE year 2 / semester 1 subjects
-- so students can compare at least three schedule options per subject.

-- CSCI2104 alternative A
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 1, TIME '08:00', TIME '10:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-427'
FROM semesters sem
JOIN subjects sub ON sub.code = 'CSCI2104'
JOIN teachers tea ON tea.email = 'r.serikbayev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1
    FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 1
      AND so.start_time = TIME '08:00'
      AND so.end_time = TIME '10:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-427'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 1, TIME '10:00', TIME '08:00', so.id, 'LECTURE', 'L-427'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2104'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '08:00'
  AND so.end_time = TIME '10:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-427'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 1
      AND mt.start_time = TIME '08:00'
      AND mt.end_time = TIME '10:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-427'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 3, TIME '13:00', TIME '12:00', so.id, 'PRACTICE', 'P-427'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2104'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '08:00'
  AND so.end_time = TIME '10:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-427'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 3
      AND mt.start_time = TIME '12:00'
      AND mt.end_time = TIME '13:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-427'
  );

-- CSCI2104 alternative B
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 0, TIME '14:00', TIME '16:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-427'
FROM semesters sem
JOIN subjects sub ON sub.code = 'CSCI2104'
JOIN teachers tea ON tea.email = 'a.nurgaliyev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1
    FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 0
      AND so.start_time = TIME '14:00'
      AND so.end_time = TIME '16:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-427'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 0, TIME '16:00', TIME '14:00', so.id, 'LECTURE', 'L-427'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2104'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-427'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 0
      AND mt.start_time = TIME '14:00'
      AND mt.end_time = TIME '16:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-427'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 2, TIME '11:00', TIME '10:00', so.id, 'PRACTICE', 'P-427'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2104'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-427'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 2
      AND mt.start_time = TIME '10:00'
      AND mt.end_time = TIME '11:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-427'
  );

-- CSCI2105 alternative A
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 1, TIME '14:00', TIME '16:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-428'
FROM semesters sem
JOIN subjects sub ON sub.code = 'CSCI2105'
JOIN teachers tea ON tea.email = 'a.nurgaliyev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 1
      AND so.start_time = TIME '14:00'
      AND so.end_time = TIME '16:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-428'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 1, TIME '16:00', TIME '14:00', so.id, 'LECTURE', 'L-428'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2105'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-428'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 1
      AND mt.start_time = TIME '14:00'
      AND mt.end_time = TIME '16:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-428'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 4, TIME '09:00', TIME '08:00', so.id, 'PRACTICE', 'P-428'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2105'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-428'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 4
      AND mt.start_time = TIME '08:00'
      AND mt.end_time = TIME '09:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-428'
  );

-- CSCI2105 alternative B
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 0, TIME '16:00', TIME '18:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-428'
FROM semesters sem
JOIN subjects sub ON sub.code = 'CSCI2105'
JOIN teachers tea ON tea.email = 'r.serikbayev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 0
      AND so.start_time = TIME '16:00'
      AND so.end_time = TIME '18:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-428'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 0, TIME '18:00', TIME '16:00', so.id, 'LECTURE', 'L-428'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2105'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '16:00'
  AND so.end_time = TIME '18:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-428'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 0
      AND mt.start_time = TIME '16:00'
      AND mt.end_time = TIME '18:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-428'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 3, TIME '11:00', TIME '10:00', so.id, 'PRACTICE', 'P-428'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'CSCI2105'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '16:00'
  AND so.end_time = TIME '18:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-428'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 3
      AND mt.start_time = TIME '10:00'
      AND mt.end_time = TIME '11:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-428'
  );

-- INFT2102 alternative A
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 1, TIME '10:00', TIME '12:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-418'
FROM semesters sem
JOIN subjects sub ON sub.code = 'INFT2102'
JOIN teachers tea ON tea.email = 'r.serikbayev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 1
      AND so.start_time = TIME '10:00'
      AND so.end_time = TIME '12:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-418'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 1, TIME '12:00', TIME '10:00', so.id, 'LECTURE', 'L-418'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2102'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '10:00'
  AND so.end_time = TIME '12:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-418'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 1
      AND mt.start_time = TIME '10:00'
      AND mt.end_time = TIME '12:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-418'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 4, TIME '15:00', TIME '14:00', so.id, 'PRACTICE', 'P-418'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2102'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '10:00'
  AND so.end_time = TIME '12:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-418'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 4
      AND mt.start_time = TIME '14:00'
      AND mt.end_time = TIME '15:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-418'
  );

-- INFT2102 alternative B
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 2, TIME '14:00', TIME '16:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-418'
FROM semesters sem
JOIN subjects sub ON sub.code = 'INFT2102'
JOIN teachers tea ON tea.email = 'a.nurgaliyev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 2
      AND so.start_time = TIME '14:00'
      AND so.end_time = TIME '16:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-418'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 2, TIME '16:00', TIME '14:00', so.id, 'LECTURE', 'L-418'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2102'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 2
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-418'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 2
      AND mt.start_time = TIME '14:00'
      AND mt.end_time = TIME '16:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-418'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 0, TIME '13:00', TIME '12:00', so.id, 'PRACTICE', 'P-418'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2102'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 2
  AND so.start_time = TIME '14:00'
  AND so.end_time = TIME '16:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-418'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 0
      AND mt.start_time = TIME '12:00'
      AND mt.end_time = TIME '13:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-418'
  );

-- INFT2205 alternative A
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 0, TIME '12:00', TIME '14:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-382'
FROM semesters sem
JOIN subjects sub ON sub.code = 'INFT2205'
JOIN teachers tea ON tea.email = 'a.nurgaliyev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 0
      AND so.start_time = TIME '12:00'
      AND so.end_time = TIME '14:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-382'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 0, TIME '14:00', TIME '12:00', so.id, 'LECTURE', 'L-382'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2205'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '12:00'
  AND so.end_time = TIME '14:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-382'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 0
      AND mt.start_time = TIME '12:00'
      AND mt.end_time = TIME '14:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-382'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 3, TIME '17:00', TIME '16:00', so.id, 'PRACTICE', 'P-382'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2205'
  AND tea.email = 'a.nurgaliyev@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '12:00'
  AND so.end_time = TIME '14:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-382'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 3
      AND mt.start_time = TIME '16:00'
      AND mt.end_time = TIME '17:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-382'
  );

-- INFT2205 alternative B
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 1, TIME '16:00', TIME '18:00', sem.id, sub.id, tea.id, 'LECTURE', 'L-382'
FROM semesters sem
JOIN subjects sub ON sub.code = 'INFT2205'
JOIN teachers tea ON tea.email = 'r.serikbayev@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 1
      AND so.start_time = TIME '16:00'
      AND so.end_time = TIME '18:00'
      AND so.lesson_type = 'LECTURE'
      AND so.room = 'L-382'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 1, TIME '18:00', TIME '16:00', so.id, 'LECTURE', 'L-382'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2205'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '16:00'
  AND so.end_time = TIME '18:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-382'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 1
      AND mt.start_time = TIME '16:00'
      AND mt.end_time = TIME '18:00'
      AND mt.lesson_type = 'LECTURE'
      AND mt.room = 'L-382'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 4, TIME '11:00', TIME '10:00', so.id, 'PRACTICE', 'P-382'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'INFT2205'
  AND tea.email = 'r.serikbayev@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '16:00'
  AND so.end_time = TIME '18:00'
  AND so.lesson_type = 'LECTURE'
  AND so.room = 'L-382'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 4
      AND mt.start_time = TIME '10:00'
      AND mt.end_time = TIME '11:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-382'
  );

-- HUM1101 alternative A
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 0, TIME '08:00', TIME '09:00', sem.id, sub.id, tea.id, 'PRACTICE', 'P-379'
FROM semesters sem
JOIN subjects sub ON sub.code = 'HUM1101'
JOIN teachers tea ON tea.email = 'a.beketov@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 0
      AND so.start_time = TIME '08:00'
      AND so.end_time = TIME '09:00'
      AND so.lesson_type = 'PRACTICE'
      AND so.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 0, TIME '09:00', TIME '08:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '08:00'
  AND so.end_time = TIME '09:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 0
      AND mt.start_time = TIME '08:00'
      AND mt.end_time = TIME '09:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 2, TIME '13:00', TIME '12:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '08:00'
  AND so.end_time = TIME '09:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 2
      AND mt.start_time = TIME '12:00'
      AND mt.end_time = TIME '13:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 3, TIME '15:00', TIME '14:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 0
  AND so.start_time = TIME '08:00'
  AND so.end_time = TIME '09:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 3
      AND mt.start_time = TIME '14:00'
      AND mt.end_time = TIME '15:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );

-- HUM1101 alternative B
INSERT INTO subject_offerings (capacity, day_of_week, start_time, end_time, semester_id, subject_id, teacher_id, lesson_type, room)
SELECT 20, 1, TIME '10:00', TIME '11:00', sem.id, sub.id, tea.id, 'PRACTICE', 'P-379'
FROM semesters sem
JOIN subjects sub ON sub.code = 'HUM1101'
JOIN teachers tea ON tea.email = 'a.beketov@kbtu.kz'
WHERE sem.name = '2026-2027 Fall'
  AND NOT EXISTS (
    SELECT 1 FROM subject_offerings so
    WHERE so.semester_id = sem.id
      AND so.subject_id = sub.id
      AND so.teacher_id = tea.id
      AND so.day_of_week = 1
      AND so.start_time = TIME '10:00'
      AND so.end_time = TIME '11:00'
      AND so.lesson_type = 'PRACTICE'
      AND so.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 1, TIME '11:00', TIME '10:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '10:00'
  AND so.end_time = TIME '11:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 1
      AND mt.start_time = TIME '10:00'
      AND mt.end_time = TIME '11:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 3, TIME '09:00', TIME '08:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '10:00'
  AND so.end_time = TIME '11:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 3
      AND mt.start_time = TIME '08:00'
      AND mt.end_time = TIME '09:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );

INSERT INTO meeting_times (day_of_week, end_time, start_time, subject_offering_id, lesson_type, room)
SELECT 4, TIME '17:00', TIME '16:00', so.id, 'PRACTICE', 'P-379'
FROM subject_offerings so
JOIN semesters sem ON sem.id = so.semester_id
JOIN subjects sub ON sub.id = so.subject_id
JOIN teachers tea ON tea.id = so.teacher_id
WHERE sem.name = '2026-2027 Fall'
  AND sub.code = 'HUM1101'
  AND tea.email = 'a.beketov@kbtu.kz'
  AND so.day_of_week = 1
  AND so.start_time = TIME '10:00'
  AND so.end_time = TIME '11:00'
  AND so.lesson_type = 'PRACTICE'
  AND so.room = 'P-379'
  AND NOT EXISTS (
    SELECT 1 FROM meeting_times mt
    WHERE mt.subject_offering_id = so.id
      AND mt.day_of_week = 4
      AND mt.start_time = TIME '16:00'
      AND mt.end_time = TIME '17:00'
      AND mt.lesson_type = 'PRACTICE'
      AND mt.room = 'P-379'
  );
