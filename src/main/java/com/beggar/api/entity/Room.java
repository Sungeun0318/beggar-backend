package com.beggar.api.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본키 자동 증가 (  1, 2, 3 .. )
    @Column(name = "room_no")
    private Long roomNo;

    @Column(nullable = false)
    private String roomName;

    @Column(nullable = false, unique = true)
    private String roomCode;

    @Column(nullable = false)
    private Long ownerUserNo;

    private Integer totalBudget; // 처음엔 비어있을 수 있으니 null 허용

    @Column(nullable = false)
    private Boolean isFriends;

    @Column(name="location" , length = 100)
    private String location;

    private LocalDateTime roomCreated;

    // DB에 저장되기 직전에 현재 시간으로 세팅해주는 함수
    @PrePersist
    public void prePersist() {
        this.roomCreated = LocalDateTime.now();
    }

    // 방 만들 때 쓸 생성자
    public Room(String roomName, String roomCode, Long ownerUserNo, Boolean isFriends) {
        this.roomName = roomName;
        this.roomCode = roomCode;
        this.ownerUserNo = ownerUserNo;
        this.isFriends = isFriends;
        this.location = location;
        this.roomCreated = LocalDateTime.now();
    }
}
