package com.varc.brewnetapp.domain.item.command.domain.aggregate.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tbl_super_category")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 자동 증가 전략
    @Column(name = "super_category_code")
    private Integer superCategoryCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "active", nullable = false)
    private Boolean active;
}