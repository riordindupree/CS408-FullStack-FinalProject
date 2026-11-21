-- Seed data from the monsters and items listed in guide.html.
BEGIN;

INSERT INTO MONSTER (monster_id, monster_name, monster_desc, monster_icon, monster_score) VALUES
    (1, 'Molynx', 'A somewhat rodent-like creature. Their poor eyesight causes them to be very opportunistic hunters.', 'ᘛ⁐̤ᕐᐷ', 2800),
    (2, 'Iktorb', 'Spider-like creatures used to capture certain creatures with its sticky webs.', 'ᕐ⚗︎', 1400),
    (3, 'Lappup', 'Dog-like creatures used to capture certain creatures by swallowing them whole.', '૮(´ ཀ `)ა', 2300),
    (4, 'Quapok', 'Large duck-like creature. Being both mean and stubborn, befriending this apex predator will take more than just feeding it a berry it likes! Great at attacking other creatures.', E'⊃ভ了\n⋂', 5500),
    (5, 'Fleastrike', 'Wool-covered parasites. Its fluffy body makes it hard for other creatures to swallow.', '☁︎੬ཐི༏ཋྀ3', 500),
    (6, 'Soupon', 'A turtle whose body is covered in an oil which makes its skin hot like magma.', '໔㈫৯', 2500),
    (7, 'Korokorobi', 'Final boss. Defeating and capturing is the goal of the game. It will take 7 hits to knock him down!', E'(｡෴｡)\n(｡෴｡)\n(ᄋ)(｡෴｡)(ᄋ)', 7800)
ON CONFLICT (monster_id) DO UPDATE SET
    monster_name = EXCLUDED.monster_name,
    monster_desc = EXCLUDED.monster_desc,
    monster_icon = EXCLUDED.monster_icon,
    monster_score = EXCLUDED.monster_score;

SELECT setval(pg_get_serial_sequence('monster', 'monster_id'), (SELECT MAX(monster_id) FROM MONSTER), TRUE);

INSERT INTO ITEM (item_id, item_name, item_desc, item_icon, item_score) VALUES
    (1, 'Salty Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ጬ', 100),
    (2, 'Spicy Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', 'ଟ', 100),
    (3, 'Twin Berry', 'Liked by different types of enemies. Can be used to befriend monsters.', '☊', 100),
    (4, 'Key 1', 'Used to unlock door 1.', '♀', 500),
    (5, 'Key 2', 'Used to unlock door 2.', '♂', 500)
ON CONFLICT (item_id) DO UPDATE SET
    item_name = EXCLUDED.item_name,
    item_desc = EXCLUDED.item_desc,
    item_icon = EXCLUDED.item_icon,
    item_score = EXCLUDED.item_score;

SELECT setval(pg_get_serial_sequence('item', 'item_id'), (SELECT MAX(item_id) FROM ITEM), TRUE);

COMMIT;