package com.florie.controller;

import com.florie.service.UserService;
import com.florie.service.FlowerService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final UserService userService;
    private final FlowerService flowerService;

    public HomeController(UserService userService, FlowerService flowerService) {
        this.userService = userService;
        this.flowerService = flowerService;
    }

    @GetMapping("/home")
    public String showHome(Principal principal, Model model) {
        model.addAttribute("nickname", userService.getNickname(principal.getName()));
        model.addAttribute("currentFlower", flowerService.getCurrentFlower(principal.getName()).orElse(null));
        return "home";
    }
}
