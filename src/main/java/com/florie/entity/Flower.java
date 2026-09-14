package com.florie.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "flowers")
public class Flower {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flower_id", nullable = false)
    private Long flowerId;

    // 子から親だけを参照し、関連先の保存・削除は連動させない。
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 子から親だけを参照し、関連先の保存・削除は連動させない。
    @ManyToOne(optional = false)
    @JoinColumn(name = "flower_type_id", nullable = false)
    private FlowerType flowerType;

    @Column(name = "flower_nickname", nullable = false, length = 20)
    private String flowerNickname;

    @Column(name = "started_on", nullable = false)
    private LocalDate startedOn;

    @Column(name = "ended_on")
    private LocalDate endedOn;

    // DBでも状態名を読めるよう、数値ではなく文字列で保存する。
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20, columnDefinition = "VARCHAR(20)")
    private FlowerStatus status;

    public Flower() {
    }

    public Long getFlowerId() {
        return flowerId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public FlowerType getFlowerType() {
        return flowerType;
    }

    public void setFlowerType(FlowerType flowerType) {
        this.flowerType = flowerType;
    }

    public String getFlowerNickname() {
        return flowerNickname;
    }

    public void setFlowerNickname(String flowerNickname) {
        this.flowerNickname = flowerNickname;
    }

    public LocalDate getStartedOn() {
        return startedOn;
    }

    public void setStartedOn(LocalDate startedOn) {
        this.startedOn = startedOn;
    }

    public LocalDate getEndedOn() {
        return endedOn;
    }

    public void setEndedOn(LocalDate endedOn) {
        this.endedOn = endedOn;
    }

    public FlowerStatus getStatus() {
        return status;
    }

    public void setStatus(FlowerStatus status) {
        this.status = status;
    }
}

