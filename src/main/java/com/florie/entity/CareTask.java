package com.florie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "care_tasks")
public class CareTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_task_id", nullable = false)
    private Long careTaskId;

    // 子から親だけを参照し、関連先の保存・削除は連動させない。
    @ManyToOne(optional = false)
    @JoinColumn(name = "flower_id", nullable = false)
    private Flower flower;

    @Column(name = "care_name", nullable = false, length = 50)
    private String careName;

    @Column(name = "interval_days", nullable = false)
    private Integer intervalDays;

    @Column(name = "next_care_date", nullable = false)
    private LocalDate nextCareDate;

    public CareTask() {
    }

    public Long getCareTaskId() {
        return careTaskId;
    }

    public Flower getFlower() {
        return flower;
    }

    public void setFlower(Flower flower) {
        this.flower = flower;
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

    public LocalDate getNextCareDate() {
        return nextCareDate;
    }

    public void setNextCareDate(LocalDate nextCareDate) {
        this.nextCareDate = nextCareDate;
    }
}

