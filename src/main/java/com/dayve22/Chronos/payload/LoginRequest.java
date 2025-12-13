package com.dayve22.Chronos.payload;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}