package com.matrix.cloud.dto;

import lombok.Data;

@Data
public class JoinMeetingRequest {
    private String roomName;
    private String password;
}
