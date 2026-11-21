package com.example.springboot.game_components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:combat-grid-test;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class GameGridCombatTest {
    // Test against real JDBC repositories so state-based SQL interactions are exercised.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private MonsterRepository monsterRepository;

    @Autowired
    private MockMvc mvc;

    // Rebuild the small catalog and interaction tables for each independent combat test.
    @BeforeEach
    void setUpDatabase() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS MONSTER_ITEM");
        jdbcTemplate.execute("DROP TABLE IF EXISTS MONSTER_MONSTER");
        jdbcTemplate.execute("DROP TABLE IF EXISTS ITEM");
        jdbcTemplate.execute("DROP TABLE IF EXISTS MONSTER");
        jdbcTemplate.execute(
            "CREATE TABLE ITEM (item_id INTEGER PRIMARY KEY, item_name VARCHAR(40), item_icon VARCHAR(40), item_score INTEGER)");
        jdbcTemplate.execute(
            "CREATE TABLE MONSTER (monster_id INTEGER PRIMARY KEY, monster_name VARCHAR(40), monster_icon VARCHAR(80), monster_score INTEGER)");
        jdbcTemplate.execute(
            "CREATE TABLE MONSTER_ITEM (enemy_monster INTEGER, player_item INTEGER, current_state CHAR, result VARCHAR(200), new_state CHAR, lose BOOLEAN, PRIMARY KEY (enemy_monster, player_item, current_state))");
        jdbcTemplate.execute(
            "CREATE TABLE MONSTER_MONSTER (enemy_monster INTEGER, player_monster INTEGER, current_state CHAR, result VARCHAR(200), new_state CHAR, lose BOOLEAN, PRIMARY KEY (enemy_monster, player_monster, current_state))");

        // Provide the catalog IDs referenced by the default GameGrid room layout.
        for (int id = 1; id <= 5; id++) {
            jdbcTemplate.update(
                "INSERT INTO ITEM (item_id, item_name, item_icon, item_score) VALUES (?, ?, ?, ?)",
                id,
                "Item " + id,
                "item-icon-" + id,
                100
            );
        }
        for (int id = 1; id <= 7; id++) {
            jdbcTemplate.update(
                "INSERT INTO MONSTER (monster_id, monster_name, monster_icon, monster_score) VALUES (?, ?, ?, ?)",
                id,
                "Monster " + id,
                "monster-icon-" + id,
                250
            );
        }

        // This fixture rule advances the Quapok fight directly to the victory choice.
        jdbcTemplate.update(
            "INSERT INTO MONSTER_ITEM VALUES (4, 1, 'a', 'Quapok defeated!', '$', FALSE)"
        );
        jdbcTemplate.update(
            "INSERT INTO MONSTER_ITEM VALUES (4, 2, 'a', 'Quapok prepared!', '2', FALSE)"
        );
        jdbcTemplate.update(
            "INSERT INTO MONSTER_ITEM VALUES (4, 2, '2', 'Quapok defeated in state two!', '$', FALSE)"
        );
        jdbcTemplate.update(
            "INSERT INTO MONSTER_MONSTER VALUES (4, 1, 'a', 'Quapok defeated by Monster 1!', '$', FALSE)"
        );
    }

    // Verify that Run preserves the enemy state and collection stores the monster in inventory.
    @Test
    void runPreservesDefeatedMonsterAndCollectAddsItToFreeInventorySlot() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{1, 0, 0, 0});
        enterQuapokRoom(grid);

        assertTrue(grid.isFighting());
        assertEquals("monster-icon-4", grid.getFightMonsterIcon());
        assertEquals("Quapok defeated!", grid.fightSelection(0));
        assertTrue(grid.isVictoryPending());

        // Fleeing exits combat but leaves the monster at its current '$' state.
        assertEquals("You ran away!", grid.runFromFight());
        assertFalse(grid.isFighting());
        assertEquals("You defeated Monster 4!", grid.roomSelection(1));
        assertTrue(grid.isVictoryPending());

        // Collecting occupies an empty slot and removes the monster from this room.
        assertEquals("Collected Monster 4!", grid.collectDefeatedMonster());
        assertEquals("monster-icon-4", grid.getInventory(1));
        assertFalse(grid.isFighting());
        assertEquals("  ", grid.getCurrentData(1));
    }

    // Verify that a full inventory keeps the victory choice active until the player makes room.
    @Test
    void collectionIsUnavailableWhenInventoryIsFull() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{1, 2, 3, 4});
        enterQuapokRoom(grid);
        grid.fightSelection(0);

        assertEquals(
            "(Your inventory is full. Toss an item before collecting this monster.)",
            grid.collectDefeatedMonster()
        );
        assertTrue(grid.isFighting());
        assertTrue(grid.isVictoryPending());
    }

    // Verify that the temporary reset action restores a clean run and starting position.
    @Test
    void resetRestoresTheStartingGameState() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{1, 2, 3, 4});
        enterQuapokRoom(grid);

        grid.reset();

        assertFalse(grid.isFighting());
        assertEquals(99, grid.getSteps());
        assertEquals("XXX", grid.getInventory(0));
        assertEquals("Coordinates: (3, 3)\nFloor: 1", grid.generic());
        assertEquals("⌧", grid.getCurrentData(0));
    }

    @Test
    void onlyRunsWithCollectedKorokorobiAreRecordedAsWins() throws Exception {
        GameGrid runWithoutKorokorobi = createGridWithInventory(new int[]{1, 0, 0, 0});
        setCoordinatesAtFirstFloorExit(runWithoutKorokorobi);

        runWithoutKorokorobi.roomSelection(3);

        assertTrue(runWithoutKorokorobi.isRunComplete());
        assertFalse(runWithoutKorokorobi.isRunWon());

        GameGrid runWithKorokorobi = createGridWithInventory(new int[]{1, -7, 0, 0});
        setCoordinatesAtFirstFloorExit(runWithKorokorobi);

        runWithKorokorobi.roomSelection(3);

        assertTrue(runWithKorokorobi.isRunComplete());
        assertTrue(runWithKorokorobi.isRunWon());
    }

    @Test
    void startNewGameActionResetsTheCurrentRun() throws Exception {
        MvcResult startResult = mvc.perform(get("/start"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Steps: <span>99</span>")))
            .andReturn();
        MockHttpSession session = (MockHttpSession) startResult.getRequest().getSession(false);

        mvc.perform(post("/start/room-selection")
            .session(session)
            .param("option", "2"))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/start").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Steps: <span>98</span>")));

        mvc.perform(post("/start/reset").session(session))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/start").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Steps: <span>99</span>")));
    }

    // Verify that inventory shows only the first line of multi-line monster art.
    @Test
    void inventoryAndFightButtonsTruncateMultiLineMonsterIconsOnly() throws Exception {
        jdbcTemplate.update(
            "UPDATE MONSTER SET monster_icon = ? WHERE monster_id = 4",
            "Quapok first line\nQuapok second line"
        );
        jdbcTemplate.update(
            "UPDATE MONSTER SET monster_icon = ? WHERE monster_id = 7",
            "Korokorobi first line\nKorokorobi second line"
        );
        jdbcTemplate.update(
            "UPDATE ITEM SET item_icon = ? WHERE item_id = 1",
            "Item first line\nItem second line"
        );
        GameGrid grid = createGridWithInventory(new int[]{-4, -7, 1, 0});

        assertEquals("Quapok first line", grid.getInventory(0));
        assertEquals("Korokorobi first line", grid.getInventory(1));
        assertEquals("Item first line\nItem second line", grid.getInventory(2));
        assertEquals("Monster 4 [ Quapok first line ]", grid.getFightOption(0));
        assertEquals("Monster 7 [ Korokorobi first line ]", grid.getFightOption(1));
        assertEquals("Item 1 [ Item first line\nItem second line ]", grid.getFightOption(2));
    }

    // Verify that a collected monster can be used as a combat option through MONSTER_MONSTER.
    @Test
    void usesMonsterVersusMonsterInteractionRules() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{-1, 0, 0, 0});
        enterQuapokRoom(grid);

        assertEquals("Quapok defeated by Monster 1!", grid.fightSelection(0));
        assertTrue(grid.isVictoryPending());
    }

    // Verify that fleeing and re-entering resumes the same non-victory monster state.
    @Test
    void runPreservesMonsterStateForTheNextEncounter() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{2, 0, 0, 0});
        enterQuapokRoom(grid);

        assertEquals("Quapok prepared!", grid.fightSelection(0));
        grid.runFromFight();
        assertEquals("You Stand Before A Wild Monster 4!", grid.roomSelection(1));
        assertFalse(grid.isVictoryPending());
        assertEquals("Quapok defeated in state two!", grid.fightSelection(0));
        assertTrue(grid.isVictoryPending());
    }

    // Verify that the web controller renders the dedicated fight template for an encounter.
    @Test
    void rendersFightScreenAfterEnteringMonsterRoom() throws Exception {
        MvcResult startResult = mvc.perform(get("/start"))
            .andExpect(status().isOk())
            .andReturn();
        MockHttpSession session = (MockHttpSession) startResult.getRequest().getSession(false);

        // Follow the same four room choices that lead from the starting room to Quapok.
        for (int option : new int[]{1, 0, 1, 1}) {
            mvc.perform(post("/start/room-selection")
                    .session(session)
                    .param("option", String.valueOf(option)))
                .andExpect(status().is3xxRedirection());
        }

        // The fight page should show the database-backed monster icon and a Run action.
        mvc.perform(get("/fight").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("monster-icon-4")))
            .andExpect(content().string(containsString("Run")))
            .andExpect(content().string(containsString("Monster 4")));
    }

    // Verify that selecting the room's Fight option does not consume a step.
    @Test
    void enteringFightDoesNotConsumeAStep() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{1, 0, 0, 0});
        grid.roomSelection(1);
        grid.roomSelection(0);
        grid.roomSelection(1);
        int stepsBeforeFight = grid.getSteps();

        grid.roomSelection(1);

        assertTrue(grid.isFighting());
        assertEquals(stepsBeforeFight, grid.getSteps());
    }

    // Verify that selecting an empty inventory slot uses the standard no-interaction message.
    @Test
    void selectingEmptyInventorySlotSaysNothingHappened() throws Exception {
        GameGrid grid = createGridWithInventory(new int[]{0, 0, 0, 0});
        enterQuapokRoom(grid);
        int stepsBeforeSelection = grid.getSteps();

        assertEquals("Nothing Happened.", grid.fightSelection(0));
        assertEquals(stepsBeforeSelection, grid.getSteps());
    }

    // Walk the default floor from its start to the room containing Quapok.
    private void enterQuapokRoom(GameGrid grid) {
        grid.roomSelection(1);
        grid.roomSelection(0);
        grid.roomSelection(1);
        grid.roomSelection(1);
    }

    // Seed inventory directly so tests focus on combat instead of the maze's item route.
    private GameGrid createGridWithInventory(int[] inventory) throws Exception {
        GameGrid grid = new GameGrid(itemRepository, monsterRepository);
        Field inventoryField = GameGrid.class.getDeclaredField("p_inv");
        inventoryField.setAccessible(true);
        inventoryField.set(grid, inventory);
        return grid;
    }

    private void setCoordinatesAtFirstFloorExit(GameGrid grid) throws Exception {
        setIntegerField(grid, "current_floor", 0);
        setIntegerField(grid, "current_x", 2);
        setIntegerField(grid, "current_y", 2);
    }

    private void setIntegerField(GameGrid grid, String fieldName, int value) throws Exception {
        Field field = GameGrid.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.setInt(grid, value);
    }
}
