package com.example.springboot;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
   * Load delete account page from delete.html
   * @return the contents of index
   */
  @GetMapping("/delete")
  public String delete(
      @RequestParam(required = false) boolean error,
      @RequestParam(required = false) boolean deleted,
      Model model) {
    model.addAttribute("deleteError", error);
    model.addAttribute("deleted", deleted);
    return "delete";
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