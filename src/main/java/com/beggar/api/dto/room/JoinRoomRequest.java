package com.beggar.api.dto.room;

import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
        @NotBlank String roomCode
) {}
