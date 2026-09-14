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
@Table(name = "care_records")
public class CareRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "care_record_id", nullable = false)
    private Long careRecordId;

    // 子から親だけを参照し、関連先の保存・削除は連動させない。
    @ManyToOne(optional = false)
    @JoinColumn(name = "care_task_id", nullable = false)
    private CareTask careTask;

    @Column(name = "completed_on", nullable = false)
    private LocalDate completedOn;

    public CareRecord() {
    }

    public Long getCareRecordId() {
        return careRecordId;
    }

    public CareTask getCareTask() {
        return careTask;
    }

    public void setCareTask(CareTask careTask) {
        this.careTask = careTask;
    }

    public LocalDate getCompletedOn() {
        return completedOn;
    }

    public void setCompletedOn(LocalDate completedOn) {
        this.completedOn = completedOn;
    }
}

