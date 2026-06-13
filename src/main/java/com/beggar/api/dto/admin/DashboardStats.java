package com.beggar.api.dto.admin;

public class DashboardStats {

    private final long totalUsers;
    private final long totalRooms;
    private final long activeRooms;
    private final long endedRooms;
    private final long deletedRooms;
    private final long totalPosts;
    private final long totalComments;
    private final long totalChats;
    private final long totalReceipts;

    public DashboardStats(
            long totalUsers,
            long totalRooms,
            long activeRooms,
            long endedRooms,
            long deletedRooms,
            long totalPosts,
            long totalComments,
            long totalChats,
            long totalReceipts
    ) {
        this.totalUsers = totalUsers;
        this.totalRooms = totalRooms;
        this.activeRooms = activeRooms;
        this.endedRooms = endedRooms;
        this.deletedRooms = deletedRooms;
        this.totalPosts = totalPosts;
        this.totalComments = totalComments;
        this.totalChats = totalChats;
        this.totalReceipts = totalReceipts;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public long getTotalRooms() {
        return totalRooms;
    }

    public long getActiveRooms() {
        return activeRooms;
    }

    public long getEndedRooms() {
        return endedRooms;
    }

    public long getDeletedRooms() {
        return deletedRooms;
    }

    public long getTotalPosts() {
        return totalPosts;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public long getTotalChats() {
        return totalChats;
    }

    public long getTotalReceipts() {
        return totalReceipts;
    }

}
