package com.florie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "flower_types")
public class FlowerType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flower_type_id", nullable = false)
    private Long flowerTypeId;

    @Column(name = "flower_name", nullable = false, length = 50, unique = true)
    private String flowerName;

    @Column(name = "illustration_path", nullable = false, length = 255)
    private String illustrationPath;

    // 水量・注意事項は、完了するタスクではなく常時案内として保持する。
    @Column(name = "care_guidance", length = 500)
    private String careGuidance;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public FlowerType() {
    }

    public Long getFlowerTypeId() {
        return flowerTypeId;
    }

    public String getFlowerName() {
        return flowerName;
    }

    public void setFlowerName(String flowerName) {
        this.flowerName = flowerName;
    }

    public String getIllustrationPath() {
        return illustrationPath;
    }

    // DBのローカル画像パスを優先する。未設定の既存マスタも、再投入せず表示できる。
    // 表示専用の値であり、illustrationPathそのものやDBは変更しない。
    public String getDisplayIllustrationPath() {
        if (illustrationPath != null && illustrationPath.matches("/images/[a-zA-Z0-9/_-]+\\.(svg|png|webp)")) {
            return illustrationPath;
        }
        if (flowerName == null) {
            return "";
        }
        switch (flowerName) {
            case "ガーベラ": return "/images/flowers/gerbera.svg";
            case "バラ": return "/images/flowers/rose.svg";
            case "チューリップ": return "/images/flowers/tulip.svg";
            case "カーネーション": return "/images/flowers/carnation.svg";
            case "ひまわり": return "/images/flowers/sunflower.svg";
            case "ダリア": return "/images/flowers/dahlia.svg";
            case "アネモネ": return "/images/flowers/anemone.svg";
            case "ラナンキュラス": return "/images/flowers/ranunculus.svg";
            default: return "";
        }
    }

    public void setIllustrationPath(String illustrationPath) {
        this.illustrationPath = illustrationPath;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public String getCareGuidance() {
        return careGuidance;
    }

    public void setCareGuidance(String careGuidance) {
        this.careGuidance = careGuidance;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
