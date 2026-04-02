-- =============================================
-- V9: Campus life reference data for dorm, food, map, and laundry
-- =============================================

-- ==================== DORM REFERENCE DATA ====================

insert into dorm_buildings (name, address, total_floors)
select seed.name, seed.address, seed.total_floors
from (
    values
        ('North Residence', '1 Satbayev St, Almaty', 5),
        ('South Residence', '3 Satbayev St, Almaty', 4)
) as seed(name, address, total_floors)
where not exists (select 1 from dorm_buildings existing where existing.name = seed.name);

insert into dorm_rooms (dorm_building_id, room_number, floor, room_type, price_per_semester, capacity, occupied, description)
select building.id, seed.room_number, seed.floor, seed.room_type::varchar, seed.price_per_semester, seed.capacity, seed.occupied, seed.description
from (
    values
        ('North Residence', '101', 1, 'DOUBLE_ROOM', 420000.00, 2, 1, 'Shared room close to the study lounge'),
        ('North Residence', '203', 2, 'SINGLE_SUITE', 690000.00, 1, 0, 'Quiet suite with extra storage'),
        ('North Residence', '305', 3, 'DOUBLE_ROOM', 440000.00, 2, 0, 'Bright room facing the courtyard'),
        ('South Residence', '114', 1, 'DOUBLE_ROOM', 395000.00, 2, 1, 'Budget-friendly room near the entrance'),
        ('South Residence', '218', 2, 'SINGLE_SUITE', 670000.00, 1, 0, 'Single room for students who prefer privacy'),
        ('South Residence', '322', 3, 'DOUBLE_ROOM', 430000.00, 2, 0, 'Renovated room near the laundry room')
) as seed(building_name, room_number, floor, room_type, price_per_semester, capacity, occupied, description)
join dorm_buildings building on building.name = seed.building_name
where not exists (
    select 1
    from dorm_rooms existing
    where existing.dorm_building_id = building.id
      and existing.room_number = seed.room_number
);

-- ==================== FOOD REFERENCE DATA ====================

insert into food_categories (name, icon, sort_order)
select seed.name, seed.icon, seed.sort_order
from (
    values
        ('Breakfast', '☕', 1),
        ('Hot Meals', '🍲', 2),
        ('Grab and Go', '🥪', 3),
        ('Drinks', '🥤', 4)
) as seed(name, icon, sort_order)
where not exists (select 1 from food_categories existing where existing.name = seed.name);

insert into food_items (category_id, name, description, price, image_url, available, popular)
select category.id, seed.name, seed.description, seed.price, seed.image_url, seed.available, seed.popular
from (
    values
        ('Breakfast', 'Cheese Omelet', 'Two-egg omelet with herbs and toast', 1350.00, null, true, true),
        ('Breakfast', 'Oatmeal Bowl', 'Warm oats with berries and honey', 990.00, null, true, false),
        ('Hot Meals', 'Chicken Teriyaki Bowl', 'Rice, grilled chicken, vegetables, teriyaki sauce', 2450.00, null, true, true),
        ('Hot Meals', 'Beef Lagman', 'Traditional noodle soup with beef and vegetables', 2190.00, null, true, true),
        ('Grab and Go', 'Turkey Club Sandwich', 'Toasted sandwich with turkey and fresh vegetables', 1490.00, null, true, false),
        ('Grab and Go', 'Caesar Wrap', 'Chicken caesar wrap with parmesan', 1590.00, null, true, true),
        ('Drinks', 'Iced Latte', 'Fresh espresso with milk over ice', 1050.00, null, true, true),
        ('Drinks', 'Fresh Orange Juice', '250ml fresh squeezed juice', 890.00, null, true, false)
) as seed(category_name, name, description, price, image_url, available, popular)
join food_categories category on category.name = seed.category_name
where not exists (select 1 from food_items existing where existing.name = seed.name);

-- ==================== CAMPUS MAP REFERENCE DATA ====================

insert into campus_buildings (name, code, description, building_type, latitude, longitude, floor_count, image_url)
select seed.name, seed.code, seed.description, seed.building_type::varchar, seed.latitude::double precision, seed.longitude::double precision, seed.floor_count, seed.image_url
from (
    values
        ('Main Academic Building', 'MAB', 'Core lecture building with large classrooms and labs', 'ACADEMIC', 43.2389::double precision, 76.8892::double precision, 5, null::varchar),
        ('Library and Learning Center', 'LLC', 'Library, study rooms, and collaborative zones', 'LIBRARY', 43.2391::double precision, 76.8898::double precision, 4, null::varchar),
        ('Campus Canteen', 'DIN', 'Main student dining hall and coffee point', 'CANTEEN', 43.2384::double precision, 76.8903::double precision, 2, null::varchar),
        ('North Residence', 'DORM-N', 'Primary dormitory building for first- and second-year students', 'DORM', 43.2379::double precision, 76.8888::double precision, 5, null::varchar),
        ('Sports Complex', 'SPRT', 'Indoor courts, gym, and event spaces', 'SPORT', 43.2382::double precision, 76.8911::double precision, 3, null::varchar)
) as seed(name, code, description, building_type, latitude, longitude, floor_count, image_url)
where not exists (select 1 from campus_buildings existing where existing.code = seed.code);

insert into campus_rooms (building_id, room_number, floor, room_type, name, description, capacity, latitude, longitude)
select building.id, seed.room_number, seed.floor, seed.room_type::varchar, seed.name, seed.description, seed.capacity, seed.latitude::double precision, seed.longitude::double precision
from (
    values
        ('MAB', 'L-101', 1, 'LECTURE_HALL', 'Foundations Lecture Hall', 'Large hall for first-year lectures', 120, null::double precision, null::double precision),
        ('MAB', 'L-202', 2, 'CLASSROOM', 'Digital Systems Classroom', 'General classroom for seminars', 40, null::double precision, null::double precision),
        ('MAB', 'L-307', 3, 'CLASSROOM', 'Algorithms Classroom', 'Upper-floor classroom near faculty offices', 36, null::double precision, null::double precision),
        ('MAB', 'LAB-12', 1, 'LAB', 'Computer Lab 12', 'Open computer lab with project workstations', 28, null::double precision, null::double precision),
        ('LLC', 'LIB-1', 1, 'LIBRARY', 'Open Library Hall', 'Main library floor with open seating', 90, null::double precision, null::double precision),
        ('LLC', 'SR-204', 2, 'OTHER', 'Silent Reading Room', 'Quiet study room for individual work', 24, null::double precision, null::double precision),
        ('DIN', 'C-101', 1, 'CANTEEN', 'Main Dining Hall', 'Primary canteen area with multiple serving lines', 180, null::double precision, null::double precision),
        ('DORM-N', 'D-12', 1, 'OTHER', 'Dorm Lobby', 'Residence lobby and check-in desk', 60, null::double precision, null::double precision),
        ('SPRT', 'GYM-1', 1, 'OTHER', 'Main Gym', 'Indoor training and fitness area', 80, null::double precision, null::double precision)
) as seed(building_code, room_number, floor, room_type, name, description, capacity, latitude, longitude)
join campus_buildings building on building.code = seed.building_code
where not exists (
    select 1
    from campus_rooms existing
    where existing.building_id = building.id
      and existing.room_number = seed.room_number
);

insert into campus_navigation_edges (from_room_id, to_room_id, from_building_id, to_building_id, distance_meters, is_accessible)
select from_room.id, to_room.id, from_building.id, to_building.id, seed.distance_meters, seed.is_accessible
from (
    values
        ('MAB', 'L-101', 'MAB', 'L-202', 40.0, true),
        ('MAB', 'L-202', 'MAB', 'L-307', 35.0, true),
        ('MAB', 'L-202', 'MAB', 'LAB-12', 25.0, true),
        ('MAB', 'L-101', 'LLC', 'LIB-1', 120.0, true),
        ('LLC', 'LIB-1', 'LLC', 'SR-204', 20.0, true),
        ('LLC', 'LIB-1', 'DIN', 'C-101', 90.0, true),
        ('DIN', 'C-101', 'DORM-N', 'D-12', 140.0, true),
        ('DIN', 'C-101', 'SPRT', 'GYM-1', 110.0, true)
) as seed(from_building_code, from_room_number, to_building_code, to_room_number, distance_meters, is_accessible)
join campus_buildings from_building on from_building.code = seed.from_building_code
join campus_buildings to_building on to_building.code = seed.to_building_code
join campus_rooms from_room on from_room.building_id = from_building.id and from_room.room_number = seed.from_room_number
join campus_rooms to_room on to_room.building_id = to_building.id and to_room.room_number = seed.to_room_number
where not exists (
    select 1
    from campus_navigation_edges existing
    where existing.from_room_id = from_room.id
      and existing.to_room_id = to_room.id
);

-- ==================== LAUNDRY REFERENCE DATA ====================

insert into laundry_rooms (name, dorm_building_id, total_machines)
select seed.name, building.id, seed.total_machines
from (
    values
        ('North Residence Laundry', 'North Residence', 6),
        ('South Residence Laundry', 'South Residence', 4)
) as seed(name, building_name, total_machines)
join dorm_buildings building on building.name = seed.building_name
where not exists (select 1 from laundry_rooms existing where existing.name = seed.name);

insert into laundry_machines (laundry_room_id, machine_number, status)
select room.id, seed.machine_number, seed.status::varchar
from (
    values
        ('North Residence Laundry', 1, 'AVAILABLE'),
        ('North Residence Laundry', 2, 'AVAILABLE'),
        ('North Residence Laundry', 3, 'IN_USE'),
        ('North Residence Laundry', 4, 'AVAILABLE'),
        ('North Residence Laundry', 5, 'OUT_OF_ORDER'),
        ('North Residence Laundry', 6, 'AVAILABLE'),
        ('South Residence Laundry', 1, 'AVAILABLE'),
        ('South Residence Laundry', 2, 'IN_USE'),
        ('South Residence Laundry', 3, 'AVAILABLE'),
        ('South Residence Laundry', 4, 'AVAILABLE')
) as seed(room_name, machine_number, status)
join laundry_rooms room on room.name = seed.room_name
where not exists (
    select 1
    from laundry_machines existing
    where existing.laundry_room_id = room.id
      and existing.machine_number = seed.machine_number
);
