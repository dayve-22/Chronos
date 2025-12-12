package com.dayve22.Chronos.payload;

import lombok.Data;

@Data
public class JwtResponse {
    private String token;
    public JwtResponse(String token) { this.token = token; }
}
