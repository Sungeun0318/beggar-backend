package com.beggar.api.dto.admin;

public class UserDetail {

    private final Long userNo;
    private final String userName;
    private final String email;
    private final String role;
    private final String genderLabel;
    private final String ageRange;
    private final String createdAt;
    private final long ownedRoomCount;
    private final long joinedRoomCount;
    private final long postCount;
    private final long commentCount;

    public UserDetail(
            Long userNo,
            String userName,
            String email,
            String role,
            String genderLabel,
            String ageRange,
            String createdAt,
            long ownedRoomCount,
            long joinedRoomCount,
            long postCount,
            long commentCount
    ) {
        this.userNo = userNo;
        this.userName = userName;
        this.email = email;
        this.role = role;
        this.genderLabel = genderLabel;
        this.ageRange = ageRange;
        this.createdAt = createdAt;
        this.ownedRoomCount = ownedRoomCount;
        this.joinedRoomCount = joinedRoomCount;
        this.postCount = postCount;
        this.commentCount = commentCount;
    }

    public Long getUserNo() {
        return userNo;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getGenderLabel() {
        return genderLabel;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public long getOwnedRoomCount() {
        return ownedRoomCount;
    }

    public long getJoinedRoomCount() {
        return joinedRoomCount;
    }

    public long getPostCount() {
        return postCount;
    }

    public long getCommentCount() {
        return commentCount;
    }
}
