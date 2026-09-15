package com.florie.controller;

import com.florie.entity.Flower;
import com.florie.service.FlowerService;
import java.security.Principal;
import java.util.Optional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GardenController {
    private final FlowerService flowerService;

    public GardenController(FlowerService flowerService) {
        this.flowerService = flowerService;
    }

    @GetMapping("/garden")
    public String showGarden(Principal principal, Model model) {
        model.addAttribute("endedFlowers", flowerService.getEndedFlowers(principal.getName()));
        return "garden";
    }

    @GetMapping("/garden/{flowerId}")
    public String showMemory(@PathVariable Long flowerId, Principal principal, Model model,
                             RedirectAttributes redirectAttributes) {
        Optional<Flower> flower = flowerService.getMemoryFlower(principal.getName(), flowerId);
        if (flower.isEmpty()) {
            // 他人の花・ACTIVE・不存在を区別せず、花の情報を表示しない。
            redirectAttributes.addFlashAttribute("gardenError", "このお花の思い出は表示できません。");
            return "redirect:/garden";
        }
        model.addAttribute("flower", flower.get());
        return "memory";
    }
}
