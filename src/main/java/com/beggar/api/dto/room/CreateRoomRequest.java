package com.beggar.api.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRoomRequest(
        @NotBlank @Size(max = 15) String roomName,
        @NotEmpty List<String> tags
) {}
