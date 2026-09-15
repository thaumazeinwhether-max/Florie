package com.florie.controller;

import com.florie.service.UserService;
import com.florie.service.FlowerService;
import com.florie.service.CareService;
import com.florie.entity.Flower;
import java.time.LocalDate;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    private final UserService userService;
    private final FlowerService flowerService;
    private final CareService careService;

    public HomeController(UserService userService, FlowerService flowerService, CareService careService) {
        this.userService = userService;
        this.flowerService = flowerService;
        this.careService = careService;
    }

    @GetMapping("/home")
    public String showHome(Principal principal, Model model) {
        model.addAttribute("nickname", userService.getNickname(principal.getName()));
        Flower flower = flowerService.getCurrentFlower(principal.getName()).orElse(null);
        model.addAttribute("currentFlower", flower);
        if (flower != null) {
            // 検索と画面の期限表示で、日付境界をまたいでも同じ「今日」を使う。
            LocalDate today = careService.getToday();
            model.addAttribute("today", today);
            model.addAttribute("dueTasks", careService.getDueTasks(principal.getName(), today));
            model.addAttribute("careDescriptions", careService.getCareDescriptions(flower));
        }
        return "home";
    }
}
