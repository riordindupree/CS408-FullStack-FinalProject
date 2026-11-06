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
        return "redirect:/start";
    }

    /**
     * Generates and sends all values to start.html used for displaying game tokens.
     * @param model : page which will be altered to contain given attributes
     * @return page being altered
     */
    @GetMapping("/start")
    public String map(
        Model model) {
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
