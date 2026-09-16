package com.florie.controller;

import com.florie.config.SecurityConfig;
import com.florie.entity.Flower;
import com.florie.entity.FlowerType;
import com.florie.service.CareService;
import com.florie.service.FlowerService;
import com.florie.service.UserService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({HomeController.class, GardenController.class, FlowerController.class})
@Import(SecurityConfig.class)
class FlowerIllustrationWebTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private FlowerService flowers;
    @MockitoBean private CareService cares;
    @MockitoBean private UserService users;

    @Test
    void eachFlowerRendersItsOwnImageOnHomeGardenAndMemory() throws Exception {
        String[] names = {"ガーベラ", "バラ", "チューリップ", "カーネーション", "ひまわり", "ダリア", "アネモネ", "ラナンキュラス"};
        String[] files = {"gerbera", "rose", "tulip", "carnation", "sunflower", "dahlia", "anemone", "ranunculus"};
        List<Flower> ended = new ArrayList<>();
        when(users.getNickname("own@example.com")).thenReturn("はな");
        when(cares.getToday()).thenReturn(LocalDate.of(2026, 9, 16));
        for (int i = 0; i < names.length; i++) {
            FlowerType type = new FlowerType();
            type.setFlowerName(names[i]);
            type.setIllustrationPath("");
            Flower flower = mock(Flower.class);
            when(flower.getFlowerId()).thenReturn((long) i + 1);
            when(flower.getFlowerType()).thenReturn(type);
            when(flower.getFlowerNickname()).thenReturn("あ".repeat(20));
            when(flower.getStartedOn()).thenReturn(LocalDate.of(2026, 9, 1));
            when(flower.getEndedOn()).thenReturn(LocalDate.of(2026, 9, 16));
            when(flowers.getCurrentFlower("own@example.com")).thenReturn(Optional.of(flower));
            when(flowers.getMemoryFlower("own@example.com", (long) i + 1)).thenReturn(Optional.of(flower));
            when(flowers.getEndedFlowers("own@example.com")).thenReturn(List.of(flower));
            String image = "src=\"/images/flowers/" + files[i] + ".svg\"";
            mvc.perform(get("/home").with(user("own@example.com")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(image)));
            mvc.perform(get("/garden").with(user("own@example.com")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(image)))
                    .andExpect(content().string(not(containsString("お花畑"))));
            MvcResult result = mvc.perform(get("/garden/" + (i + 1)).with(user("own@example.com")))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(image)))
                    .andExpect(content().string(containsString("あ".repeat(20)))).andReturn();
            if (i == 1) {
                preview("memory-long", result);
            }
            ended.add(flower);
        }
        when(flowers.getEndedFlowers("own@example.com")).thenReturn(ended);
        preview("garden-eight", mvc.perform(get("/garden").with(user("own@example.com")))
                .andExpect(status().isOk()).andReturn());
    }

    @Test
    void bundledImagesCanBeLoadedWithoutAuthentication() throws Exception {
        for (String name : new String[]{"gerbera", "rose", "tulip", "carnation", "sunflower", "dahlia", "anemone", "ranunculus"}) {
            mvc.perform(get("/images/flowers/" + name + ".svg"))
                    .andExpect(status().isOk()).andExpect(content().string(containsString("<svg")));
        }
    }

    @Test
    void storedIllustrationPathIsUsedByTemplate() throws Exception {
        FlowerType type = new FlowerType();
        type.setFlowerName("バラ");
        type.setIllustrationPath("/images/flowers/rose.svg");
        Flower flower = new Flower();
        flower.setFlowerType(type);
        flower.setFlowerNickname("ばら");
        when(flowers.getMemoryFlower("own@example.com", 42L)).thenReturn(Optional.of(flower));
        mvc.perform(get("/garden/42").with(user("own@example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"/images/flowers/rose.svg\"")));
    }

    private void preview(String name, MvcResult result) throws Exception {
        Files.createDirectories(Path.of("target/ui-preview"));
        Files.writeString(Path.of("target/ui-preview/" + name + ".html"), result.getResponse().getContentAsString());
    }
}
