package com.florie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "care_templates")
public class CareTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_template_id", nullable = false)
    private Long careTemplateId;

    // 子から親だけを参照し、関連先の保存・削除は連動させない。
    @ManyToOne(optional = false)
    @JoinColumn(name = "flower_type_id", nullable = false)
    private FlowerType flowerType;

    @Column(name = "care_name", nullable = false, length = 50)
    private String careName;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    public CareTemplate() {
    }

    public Long getCareTemplateId() {
        return careTemplateId;
    }

    public FlowerType getFlowerType() {
        return flowerType;
    }

    public void setFlowerType(FlowerType flowerType) {
        this.flowerType = flowerType;
    }

    public String getCareName() {
        return careName;
    }

    public void setCareName(String careName) {
        this.careName = careName;
    }

    public Integer getIntervalDays() {
        return intervalDays;
    }

    public void setIntervalDays(Integer intervalDays) {
        this.intervalDays = intervalDays;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}

