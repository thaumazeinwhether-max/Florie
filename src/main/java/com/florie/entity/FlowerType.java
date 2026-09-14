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
