package com.example.springboot;

import com.example.springboot.game_components.GameGrid;

import ch.qos.logback.core.model.Model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
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
        return "redirect:/start";
    }

    /**
     * Gets current floor icons and displays them in the proper quadrant.
     */
    /*@GetMapping("/map")
    public String map(
        @RequestParam(defaultValue = "1") int floor,
        Model model) {
        
    }*/
}
