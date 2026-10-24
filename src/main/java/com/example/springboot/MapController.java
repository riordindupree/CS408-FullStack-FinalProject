package com.example.springboot;

import java.util.ArrayList;
import java.util.List;

import com.example.springboot.game_components.GameGrid;
import com.example.springboot.game_components.ItemRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class MapController {

  private static final int FLOOR_COUNT = 7;
  private static final int GRID_SIZE = 5;

  private final ItemRepository itemRepository;

  public MapController(ItemRepository itemRepository) {
    this.itemRepository = itemRepository;
  }

  @GetMapping("/map")
  public String map(
      @RequestParam(defaultValue = "1") int floor,
      Model model) {
    int selectedFloor = Math.max(1, Math.min(floor, FLOOR_COUNT));
    GameGrid gameGrid = new GameGrid(itemRepository);
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
