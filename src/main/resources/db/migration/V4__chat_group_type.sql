-- Add GROUP type to chat_rooms type constraint
ALTER TABLE chat_rooms DROP CONSTRAINT IF EXISTS chat_rooms_type_check;
ALTER TABLE chat_rooms ADD CONSTRAINT chat_rooms_type_check
    CHECK (type IN ('SECTION', 'DIRECT', 'GROUP'));
