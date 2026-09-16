package com.florie.entity;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FlowerIllustrationTest {
    @Test
    void eightFlowersHaveDistinctLocalSvgFilesWithoutChangingStoredPath() throws Exception {
        String[] names = {"ガーベラ", "バラ", "チューリップ", "カーネーション", "ひまわり", "ダリア", "アネモネ", "ラナンキュラス"};
        String[] files = {"gerbera", "rose", "tulip", "carnation", "sunflower", "dahlia", "anemone", "ranunculus"};
        Set<String> drawings = new HashSet<>();
        for (int i = 0; i < names.length; i++) {
            FlowerType type = new FlowerType();
            type.setFlowerName(names[i]);
            type.setIllustrationPath("");
            assertThat(type.getDisplayIllustrationPath()).isEqualTo("/images/flowers/" + files[i] + ".svg");
            String svg = Files.readString(Path.of("src/main/resources/static" + type.getDisplayIllustrationPath()));
            assertThat(svg).contains("<svg", "viewBox=").doesNotContain("<script", "<image", "<foreignObject");
            drawings.add(svg);
            assertThat(type.getIllustrationPath()).isEmpty();
        }
        assertThat(drawings).hasSize(8);
    }

    @Test
    void storedLocalPathTakesPriority() {
        FlowerType type = new FlowerType();
        type.setFlowerName("バラ");
        type.setIllustrationPath("/images/flowers/gerbera.svg");
        assertThat(type.getDisplayIllustrationPath()).isEqualTo("/images/flowers/gerbera.svg");
        assertThat(type.getIllustrationPath()).isEqualTo("/images/flowers/gerbera.svg");
    }

    @Test
    void externalAndInvalidPathsUseBundledFlowerInstead() {
        FlowerType type = new FlowerType();
        type.setFlowerName("バラ");
        for (String path : new String[]{"https://example.com/rose.svg", "//example.com/a.svg", "/images/../secret.svg", " "}) {
            type.setIllustrationPath(path);
            assertThat(type.getDisplayIllustrationPath()).isEqualTo("/images/flowers/rose.svg");
        }
    }

    @Test
    void unknownFlowerDoesNotPretendToBeAnotherType() {
        FlowerType type = new FlowerType();
        assertThat(type.getDisplayIllustrationPath()).isEmpty();
        type.setFlowerName("未設定");
        assertThat(type.getDisplayIllustrationPath()).isEmpty();
    }

    @Test
    void templatesContainNoOldGardenNameOrPlaceholder() throws Exception {
        try (var files = Files.walk(Path.of("src/main/resources/templates"))) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                assertThat(Files.readString(file)).doesNotContain("お花畑", "flowerPlaceholder", "flower-placeholder");
            }
        }
    }
}
