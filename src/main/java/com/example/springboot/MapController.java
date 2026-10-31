package com.example.springboot;

import java.util.ArrayList;
import java.util.List;

import com.example.springboot.game_components.GameGrid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Handles requests for the dungeon map page.
 */
@Controller
public class MapController {

  private static final int FLOOR_COUNT = 7;
  private static final int GRID_SIZE = 5;

  private final GameGrid gameGrid;

  /**
   * Uses the current user's session-scoped game grid.
   */
  public MapController(GameGrid gameGrid) {
    this.gameGrid = gameGrid;
  }

  /**
   * Builds the selected floor's map from the user's current game grid.
   */
  @GetMapping("/map")
  public String map(
      @RequestParam(required = false) Integer floor,
      Model model) {
    int selectedFloor = floor == null
        ? gameGrid.getCurrentFloor()
        : Math.max(1, Math.min(floor, FLOOR_COUNT));
    List<List<String>> mapRows = new ArrayList<>();

    for (int row = 0; row < GRID_SIZE; row++) {
      List<String> mapRow = new ArrayList<>();
      for (int column = 0; column < GRID_SIZE; column++) {
        mapRow.add(gameGrid.tileToString(selectedFloor - 1, row, column));
      }
      mapRows.add(mapRow);
    }

    model.addAttribute("mapRows", mapRows);
    model.addAttribute("selectedFloor", selectedFloor);
    model.addAttribute("floors", new int[] {1, 2, 3, 4, 5, 6, 7});
    return "map";
  }
}
