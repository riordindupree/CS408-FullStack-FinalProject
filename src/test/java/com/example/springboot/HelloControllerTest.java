package com.example.springboot;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class HelloControllerTest {

  @Autowired
  private MockMvc mvc;
  

  /*@Test
  public void getHello() throws Exception {
    mvc.perform(get("/").accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().string(equalTo("<h5 class=\"text text-center text-success\">Hello World!</h5>")));
  }*/

  @Test
  public void getHealth() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status", is("UP")));
  }

  @Test
  public void homePageShowsGettingStartedAndAccountOptions() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Important Info Before Getting Started")))
        .andExpect(content().string(
            org.hamcrest.Matchers.containsString("Embark On Your Adventure!")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/about\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/guide\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/map\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/start/reset\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/login\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/account/create\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("w-100 py-3 fs-5")));
  }
}
