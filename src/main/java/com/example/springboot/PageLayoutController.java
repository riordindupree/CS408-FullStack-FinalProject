package com.example.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageLayoutController {

  /**
   * Load landing page from index.html
   * @return the contents of index
   */
  @GetMapping("/")
  public String index() {
    return "index";
  }

  /**
   * Load game page from start.html
   * @return the contents of index
   */
  @GetMapping("/start")
  public String game_board() {
    return "start";
  }

  /**
   * Load delete account page from delete.html
   * @return the contents of index
   */
  @GetMapping("/delete")
  public String delete() {
    return "delete";
  }

  /**
   * Load guide page from guide.html
   * @return the contents of index
   */
  @GetMapping("/guide")
  public String guide() {
    return "guide";
  }

  /**
   * Load about page from about.html
   * @return the contents of index
   */
  @GetMapping("/about")
  public String about() {
    return "about";
  }
}