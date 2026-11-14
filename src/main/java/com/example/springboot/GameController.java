package com.example.springboot;

import com.example.springboot.game_components.GameGrid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles actions submitted from the game page.
 */
@Controller
public class GameController {

    private final GameGrid gameGrid;
    private String game_text;

    /**
     * Uses the current user's session-scoped game grid.
     * @param gameGrid
     */
    public GameController(GameGrid gameGrid) {
        this.gameGrid = gameGrid;
    }

    /**
     * Passes the selected room option to GameGrid, where game behavior is handled.
     * @param option : button pressed
     * @return message telling map page to reload
     */
    @PostMapping("/start/room-selection")
    public String roomSelection(@RequestParam int option) {
        game_text = gameGrid.roomSelection(option);
        // Fight actions enter the dedicated combat view rather than returning to the map.
        if (gameGrid.isFighting()) {
            return "redirect:/fight";
        }
        // call sends an update to the page which tells the server to reload.
        return "redirect:/start?moved=true";
    }

    /**
     * Tosses an item when the toss button is pressed.
     * @param option : where in inventory a value should be tossed
     * @return empty string
     */
    @PostMapping("/start/toss_item")
    public String tossItem(@RequestParam int option) {
        gameGrid.tossInventory(option);
        // call sends an update to the page which tells the server to reload.
        // Keep the inventory visible on the fight page when a player frees a slot there.
        return gameGrid.isFighting() ? "redirect:/fight" : "redirect:/start";
    }

    // Applies the selected inventory choice against the active monster.
    @PostMapping("/fight/option")
    public String fightOption(@RequestParam int option) {
        game_text = gameGrid.fightSelection(option);
        // Notify an open map tab when a completed interaction changes the room.
        return gameGrid.isFighting() ? "redirect:/fight" : "redirect:/start?moved=true";
    }

    // Ends combat and returns to the same room without changing the monster's state.
    @PostMapping("/fight/run")
    public String runFromFight() {
        game_text = gameGrid.runFromFight();
        // Refresh any open map tab after returning from the fight.
        return "redirect:/start?moved=true";
    }

    // Collects a defeated monster, staying in combat if the inventory has no free slot.
    @PostMapping("/fight/collect")
    public String collectMonster() {
        game_text = gameGrid.collectDefeatedMonster();
        if (gameGrid.isFighting()) {
            return "redirect:/fight";
        }
        // Refresh any open map tab after the collected monster leaves its room.
        return "redirect:/start?moved=true";
    }

    // Resets the current session's grid and returns the player to the starting room.
    @PostMapping("/start/reset")
    public String resetGame() {
        gameGrid.reset();
        game_text = gameGrid.generic();
        // Refresh any open map tab to show the reset starting position.
        return "redirect:/start?moved=true";
    }

    // Builds the fight board and offers inventory-based combat actions.
    @GetMapping("/fight")
    public String fight(Model model) {
        if (!gameGrid.isFighting()) {
            return "redirect:/start";
        }
        model.addAttribute("steps", gameGrid.getSteps());
        model.addAttribute("item_1", gameGrid.getInventory(0));
        model.addAttribute("item_2", gameGrid.getInventory(1));
        model.addAttribute("item_3", gameGrid.getInventory(2));
        model.addAttribute("item_4", gameGrid.getInventory(3));
        model.addAttribute("top", gameGrid.getFightMonsterIcon());
        model.addAttribute("left", null);
        model.addAttribute("right", null);
        model.addAttribute("bottom", gameGrid.getFightPlayerIcon());
        model.addAttribute("game_text", game_text);
        // Victory choices replace inventory actions after an interaction defeats the monster.
        model.addAttribute("victoryPending", gameGrid.isVictoryPending());
        model.addAttribute("canCollect", gameGrid.hasFreeInventorySlot());
        model.addAttribute("option_1", gameGrid.getFightOption(0));
        model.addAttribute("option_2", gameGrid.getFightOption(1));
        model.addAttribute("option_3", gameGrid.getFightOption(2));
        model.addAttribute("option_4", gameGrid.getFightOption(3));
        return "fight";
    }

    /**
     * Generates and sends all values to start.html used for displaying game tokens.
     * @param model : page which will be altered to contain given attributes
     * @return page being altered
     */
    @GetMapping("/start")
    public String map(
        Model model) {
        // Keep direct visits to /start inside combat until the player runs or collects.
        if (gameGrid.isFighting()) {
            return "redirect:/fight";
        }
        if (game_text == null) {
            game_text = gameGrid.generic();
        }
        // get steps
        model.addAttribute("steps", gameGrid.getSteps());
        // get values for item buttons
        model.addAttribute("item_1", gameGrid.getInventory(0));
        model.addAttribute("item_2", gameGrid.getInventory(1));
        model.addAttribute("item_3", gameGrid.getInventory(2));
        model.addAttribute("item_4", gameGrid.getInventory(3));
        // get values for main game table
        model.addAttribute("top", gameGrid.getCurrentData(0));
        model.addAttribute("left", gameGrid.getCurrentData(1));
        model.addAttribute("right", gameGrid.getCurrentData(2));
        model.addAttribute("bottom", gameGrid.getCurrentData(3));
        // get textbox contents
        model.addAttribute("game_text", game_text);
        // get values for buttons
        model.addAttribute("option_1", gameGrid.getGameMessage(0));
        model.addAttribute("option_2", gameGrid.getGameMessage(1));
        model.addAttribute("option_3", gameGrid.getGameMessage(2));
        model.addAttribute("option_4", gameGrid.getGameMessage(3));
        return "start";
    }
}
