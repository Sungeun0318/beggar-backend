package com.beggar.api.dto.room;

import java.util.List;

public class RoomCreateRequest {
    private String roomName;
    private List<String> tags;
    private Boolean isFriends;

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Boolean getIsFriends() {
        return isFriends;
    }

    public void setFriends(Boolean friends) {
        isFriends = friends;
    }
}
