package com.beggar.api.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class Budget {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long budgetNo;

    private Long roomNo;
    private Long userNo;
    private Integer amount;


    public Budget(Long roomNo, Long userNo, Integer amount) {
        this.roomNo = roomNo;
        this.userNo = userNo;
        this.amount = amount;
    }

    public void updateAmount(Integer amount) {
        this.amount = amount;
    }
}