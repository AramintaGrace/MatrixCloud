package com.matrix.cloud.dto;

import lombok.Data;

@Data
public class CreateMeetingRequest {
    private String roomName;
    private String password;
    private String meetingType;
}
