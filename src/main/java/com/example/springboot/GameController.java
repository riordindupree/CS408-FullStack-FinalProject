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

    /**
     * Uses the current user's session-scoped game grid.
     */
    public GameController(GameGrid gameGrid) {
        this.gameGrid = gameGrid;
    }

    /**
     * Passes the selected room option to GameGrid, where game behavior is handled.
     */
    @PostMapping("/start/room-selection")
    public String roomSelection(@RequestParam int option) {
        gameGrid.roomSelection(option);
        // call sends an update to the page which tells the server to reload.
        return "redirect:/start?moved=true";
    }

    /**
     * Gets current floor icons and displays them in the proper quadrant.
     */
    @GetMapping("/start")
    public String map(
        Model model) {
        // get values for main game table
        model.addAttribute("top", gameGrid.getCurrentData(0));
        model.addAttribute("left", gameGrid.getCurrentData(1));
        model.addAttribute("right", gameGrid.getCurrentData(2));
        model.addAttribute("bottom", gameGrid.getCurrentData(3));
        // get values for buttons
        model.addAttribute("option_1", gameGrid.getGameMessage(0));
        model.addAttribute("option_2", gameGrid.getGameMessage(1));
        model.addAttribute("option_3", gameGrid.getGameMessage(2));
        model.addAttribute("option_4", gameGrid.getGameMessage(3));
        // get steps
        model.addAttribute("steps", gameGrid.getSteps());
        return "start";
    }
}
