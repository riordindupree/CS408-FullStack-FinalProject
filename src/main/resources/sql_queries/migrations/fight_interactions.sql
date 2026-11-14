-- Versioned migration: add the fight interaction data to existing databases.
-- Conflict updates make this safe if rows were inserted before migration tracking.
-- Fight Screen Interactions
-- NOTE:
--  * For all monsters, 'a' is the starting state.
--  * Catchable state will always be '!'.
--  * Caught state will always be '$'.
--  * Failed captures will always be 'x'.
--  * If an interaction does not exist, default response is 'Nothing Happened'.
--    The player does not lose a step.
--  * If the current state in the key is marked as '0', that means the given
--    interaction will always happen, regardless of state.

-- Store each enemy response by enemy, player item/monster, and enemy state.
INSERT INTO MONSTER_MONSTER (enemy_monster, player_monster, current_state, result, new_state, lose) VALUES
    -- Molynx (1) Interactions
    (1, 2, 'a', 'Molynx Ate Iktorb!', NULL, TRUE),
    (1, 4, '0', 'Molynx Ran Away!', 'x', FALSE),
    (1, 5, 'a', 'Molynx Tried To Swallow Fleastrike! It Cannot Stomache The Fluff! Molynx Is Vulnerable!', '!', TRUE),
    (1, 6, 'a', 'Molynx Tried To Swallow Soupon! It Burned Its Tounge! Molynx Is Vulnerable!', '!', FALSE),
    (1, 7, '0', 'Molynx Ran Away!', 'x', FALSE),
    (1, 2, '!', 'Molynx Was Caught!', '$', FALSE),
    (1, 3, '!', 'Molynx Was Caught!', '$', FALSE),

    -- Iktorkb (2) Interactions
    (2, 1, '0', 'Your Molynx Ate The Iktorb!', 'x', FALSE),
    (2, 2, '0', 'The Net Failed! (How Are You Supposed To Catch A Spider With Its Own Web?)', NULL, FALSE),
    (2, 3, '0', 'Iktorb Was Caught!', '$', FALSE),
    (2, 4, '0', 'Your Quapok Ate The Iktorb!', 'x', FALSE),
    (2, 5, '0', 'Iktorb Accepted Your Offering! Iktorb Was Caught!', '$', TRUE),
    (2, 7, '0', 'Iktorb Accepted Your Offering! Iktorb Was Caught!', '$', TRUE),

    -- Lappup (3) Interactions
    (3, 1, '0', 'Lappup Swallowed The Molynx And Ran Off!', 'x', TRUE),
    (3, 2, 'a', 'Lappup Swallowed The Iktorb And Ran Off!', 'x', TRUE),
    (3, 2, '1', 'Lappup Was Caught!', '$', FALSE),
    (3, 2, '2', 'Lappup Was Caught!', '$', FALSE),
    (3, 3, '0', 'The Lappups Started Playing!', NULL, FALSE),
    (3, 4, '0', 'Lappup Ran Away!', 'x', FALSE),
    (3, 5, '0', 'Lappup Looked At You Curiously.', NULL, FALSE),
    (3, 6, '0', 'Lappup Looked At You Curiously.', NULL, FALSE),
    (3, 7, 'a', 'Korokorobi Told Lappup To Sit.', 's', FALSE),
    (3, 7, 's', 'Korokorobi Told Lappup To Beg.', 'b', FALSE),
    (3, 7, 'b', 'Korokorobi Told Lappup To Roll Over.', 'r', FALSE),
    (3, 7, 'r', 'Korokorobi Told Lappup To Fetch.', 'f', FALSE),
    (3, 7, 'f', 'Good Boy!', '$', FALSE),

    -- Quapok (4) Interactions
    (4, 1, 'a', 'Molynx Attacked The Quapok! The Quapok Is Enraged!', '1', FALSE),
    (4, 2, 'a', 'Quapok Gazed Hungrily Toward The Iktorb! The Quapok Prepared To Attack!', '2', FALSE),
    (4, 3, 'a', 'Quapok Gazed Hungrily Toward The Lappup! The Quapok Prepared To Attack!', '2', FALSE),
    (4, 4, 'a', 'Quapok Attacked The Quapok! The Quapok Is Enraged!', '1', FALSE),
    (4, 5, 'a', 'Quapok Ran Away!', 'x', FALSE),
    (4, 6, 'a', 'Quapok Stomped On The Soupon! Soupon Was Knocked Out!', NULL, TRUE),
    (4, 7, 'a', 'Quapok Gazed Toward Korokorbi! The Quapok Prepared To Attack!', '2', FALSE),
    -- attack enraged
    (4, 1, '1', 'The Quapok Stomped! Molynx Was Defeated!', 'a', TRUE),
    (4, 2, '1', 'The Quapok Stomped! Iktrob Was Defeated!', 'a', TRUE),
    (4, 3, '1', 'The Quapok Stomped! Lappup Was Defeated!', 'a', TRUE),
    (4, 4, '1', 'The Quapok Stomped! Quapok Was Defeated!', 'a', TRUE),
    (4, 5, '1', 'Fleastrikes Lunged Toward The Quapok! Quapok Grew Ill From The Parasite! Quapok Is Vulnerable!', '!', FALSE),
    (4, 6, '1', 'The Quapok Stomped! Soupon Was Defeated!', 'a', TRUE),
    (4, 7, '1', 'Korokorobi Told Quapok To Chill Out! It Did Not Work!', 'a', TRUE),
    -- attack hungry
    (4, 1, '2', 'The Quapok Lunged With Its Mouth Wide Open! Molynx Was Defeated!', 'a', TRUE),
    (4, 2, '2', 'The Quapok Lunged With Its Mouth Wide Open! Iktrob Was Defeated!', 'a', TRUE),
    (4, 3, '2', 'The Quapok Lunged With Its Mouth Wide Open! Lappup Was Defeated!', 'a', TRUE),
    (4, 4, '2', 'The Quapok Backed Off!', 'a', FALSE),
    (4, 5, '2', 'The Quapok Lunged With Its Mouth Wide Open! Fleastrike Was Swallowed! Quapok Is Vulnerable!', '!', TRUE),
    (4, 6, '2', 'The Quapok Lunged With Its Mouth Wide Open! Quapok Was Burned! Quapok Is Vulnerable!', '!', FALSE),
    (4, 7, '2', 'Korokorobi Told Quapok To Chill Out! Quapok Chilled Out!', '$', FALSE),
    -- dazed
    (4, 2, '!', 'Quapok Was Caught!', '$', FALSE),
    (4, 3, '!', 'Lappup Tried To Catch The Quapok! It Was Too Big! Quapok Is No Longer Vulnerable!', 'a', FALSE),

    -- Fleastrike (5) Interactions
    (5, 1, '0', 'Your Molynx Ate The Fleastrike!', 'x', FALSE),
    (5, 2, '0', 'Fleastrike Was Caught!', '$', FALSE),
    (5, 3, '0', 'Lappup Refused!', NULL, FALSE),
    (5, 4, '0', 'Fleastrike Lunged Toward The Quapok! Quapok Grew Ill From The Parasite!', NULL, TRUE),
    (5, 5, '0', 'An Infestation!', '$', FALSE),
    (5, 7, '0', 'Fleastrike and Korokorbi Sat Down For A Nice Cup Of Tea!', '$', FALSE),

    -- Soupon (6) Interactions
    (6, 1, '0', 'Molynx Was Burned!', NULL, TRUE),
    (6, 2, '0', 'Iktrob Was Burned!', NULL, TRUE),
    (6, 3, 'a', 'Lappup Was Burned!', NULL, TRUE),
    (6, 4, '0', 'Quapok Whacked The Soupon! Soupon Is Vulberable!', '!', FALSE),
    (6, 5, '0', 'Fleastrike Was Burned!', NULL, TRUE),
    (6, 6, '0', 'Turtle Soup!', 't', FALSE),
    (6, 7, '0', 'Korokorobi Whacked The Soupon! Soupon Is Vulnerable!', '!', FALSE),
    (6, 3, '!', 'Soupon Was Caught!', '$', FALSE),
    (6, 3, 't', 'Soup Was Caught!', 'x', FALSE),
    (6, 7, 't', 'Korokorobi Ate The Soup!', 'x', FALSE),

    -- Korokorobi (7) Interactions
    -- 1
    (7, 1, 'a', 'Korokorobi Was Knocked Over Twice! It Hurriedly Got Back Up Again!', 'c', FALSE),
    (7, 4, 'a', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'b', FALSE),
    (7, 6, 'a', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'b', FALSE),
    -- 2
    (7, 1, 'b', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'c', FALSE),
    (7, 2, 'b', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'c', FALSE),
    (7, 4, 'b', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'c', FALSE),
    (7, 6, 'b', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'c', FALSE),
    -- 3
    (7, 1, 'c', 'Korokorobi Was Knocked Over! Korokorobi Attacked!', 'd', TRUE),
    (7, 3, 'c', 'Korokorobi Was Knocked Over! Lappup Dodged The Counter Attack!', 'd', FALSE),
    (7, 4, 'c', 'Korokorobi Was Knocked Over! Korokorobi Attacked!', 'd', TRUE),
    (7, 6, 'c', 'Korokorobi Was Knocked Over! Korokorobi Attacked!', 'd', TRUE),
    -- 4
    (7, 1, 'd', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'e', FALSE),
    (7, 4, 'd', 'Korokorobi Was Knocked Over Twice! It Hurriedly Got Back Up Again!', 'g', FALSE),
    (7, 6, 'd', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'e', FALSE),
    -- 5
    (7, 1, 'e', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'f', FALSE),
    (7, 4, 'e', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'f', FALSE),
    (7, 5, 'e', 'Korokorobi Crushed The Fleastrike!', NULL, TRUE),
    (7, 6, 'e', 'Korokorobi Was Knocked Over! It Hurriedly Got Back Up Again!', 'f', FALSE),
    -- 6
    (7, 1, 'f', 'Korokorobi Was Knocked Over! Korokorobi Attacked!', 'g', TRUE),
    (7, 4, 'f', 'Korokorobi Was Knocked Over! Korokorobi Attacked!', 'g', TRUE),
    (7, 6, 'f', 'Korokorobi Was Knocked Over! Soupon Dodged The Counter Attack!', 'g', FALSE),
    -- 7
    (7, 1, 'g', 'Korokorobi Was Knocked Over! Korokorobi Is Vulnerable!', '!', FALSE),
    (7, 4, 'g', 'Korokorobi Was Knocked Over! Korokorobi Is Vulnerable!', '!', FALSE),
    (7, 6, 'g', 'Korokorobi Was Knocked Over! Korokorobi Is Vulnerable!', '!', FALSE),
    (7, 7, 'g', 'Nanakorobi Yaoki: Fall Down 7 Times, Stand Up 8.', '$', FALSE),
    -- Catch!
    (7, 2, '!', 'Korokorobi Was Caught!', '$', FALSE),
    (7, 3, '!', 'Korokorobi Was Caught!', '$', FALSE)
ON CONFLICT (enemy_monster, player_monster, current_state) DO UPDATE
SET result = EXCLUDED.result,
    new_state = EXCLUDED.new_state,
    lose = EXCLUDED.lose;

-- Store each enemy response to an item using the same state-based rule format.
INSERT INTO MONSTER_ITEM (enemy_monster, player_item, current_state, result, new_state, lose) VALUES
    -- Molynx (1) Interactions
    (1, 1, 'a', 'Molynx Ate The Salty Berry!', NULL, TRUE),
    (1, 2, 'a', 'Molynx Ate The Spicy Berry!', NULL, TRUE),
    (1, 3, 'a', 'Molynx Ate The Twin Berry!', NULL, TRUE),
    (1, 4, 'a', 'Molynx Ate The Key!', NULL, TRUE),
    (1, 5, 'a', 'Molynx Ate The Key!', NULL, TRUE),

    -- Iktrob (2) Interactions
    (2, 3, 'a', 'Iktorb Accepted Your Offering! Iktorb Was Caught!', '$', TRUE),

    -- Lappup (3) Interactions
    (3, 1, 'a', 'Lappup Ate The Salty Berry!', '1', TRUE),
    (3, 2, 'a', 'Lappup Ate The Spicy Berry!', '1', TRUE),
    (3, 3, 'a', 'Lappup Ate The Twin Berry!', '1', TRUE),
    (3, 1, '1', 'Lappup Ate The Salty Berry!', '2', TRUE),
    (3, 2, '1', 'Lappup Ate The Spicy Berry!', '2', TRUE),
    (3, 3, '1', 'Lappup Ate The Twin Berry!', '2', TRUE),
    (3, 1, '2', 'Lappup Ate The Salty Berry! Lappup Was Caught!', '$', TRUE),
    (3, 2, '2', 'Lappup Ate The Spicy Berry! Lappup Was Caught!', '$', TRUE),
    (3, 3, '2', 'Lappup Ate The Twin Berry! Lappup Was Caught!', '$', TRUE),

    -- Quapok (4) Interactions
    (4, 1, 'a', 'Quapok Gazed Hungrily Toward The Salty Berry! The Quapok Prepared To Attack!', '2', FALSE),
    (4, 2, 'a', 'Quapok Ignored The Spicy Berry...', NULL, FALSE),
    (4, 3, 'a', 'Quapok Ignored The Twin Berry...', NULL, FALSE),
    -- berry responses
    (4, 1, '2', 'Quapok Ate The Salty Berry!', 'a', TRUE),
    (4, 2, '2', 'Quapok Ate The Spicy Berry! It Cannot Handle The Spice! Quapok Is Dazed!', '!', TRUE),

    -- Fleastrike (5) Interactions
    (5, 1, '0', 'Fleastrike Ate The Salty Berry! Fleastrike Was Caught!', '$', TRUE),

    -- Soupon (6) Interactions
    (6, 2, 'a', 'Soupon Ate The Spicy Berry! Soupon Is Vulnerable', '!', TRUE),
    (6, 2, '!', 'Soupon Was Caught!', '$', TRUE)
ON CONFLICT (enemy_monster, player_item, current_state) DO UPDATE
SET result = EXCLUDED.result,
    new_state = EXCLUDED.new_state,
    lose = EXCLUDED.lose;
