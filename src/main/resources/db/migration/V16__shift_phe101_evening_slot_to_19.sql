-- Shift the seeded evening Physical Education I slot from 18:00-19:00 to 19:00-20:00
-- so the timetable better matches the desired current-semester schedule.

UPDATE meeting_times mt
SET start_time = TIME '19:00',
    end_time = TIME '20:00'
FROM subject_offerings so
JOIN subjects sub ON sub.id = so.subject_id
WHERE mt.subject_offering_id = so.id
  AND sub.code = 'PHE101'
  AND mt.start_time = TIME '18:00'
  AND mt.end_time = TIME '19:00';

UPDATE subject_offerings so
SET start_time = TIME '19:00',
    end_time = TIME '20:00'
FROM subjects sub
WHERE sub.id = so.subject_id
  AND sub.code = 'PHE101'
  AND so.start_time = TIME '18:00'
  AND so.end_time = TIME '19:00';
