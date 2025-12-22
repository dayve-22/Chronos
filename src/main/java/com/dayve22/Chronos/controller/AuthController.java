package com.dayve22.Chronos.controller;

import com.dayve22.Chronos.dto.AuthResponse;
import com.dayve22.Chronos.dto.LoginRequest;
import com.dayve22.Chronos.dto.RegisterRequest;
import com.dayve22.Chronos.security.JwtService;
import com.dayve22.Chronos.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            String token = jwtService.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "User registered successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse(null, "Registration failed: " + e.getMessage())
            );
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            authService.authenticate(request);
            String token = jwtService.generateToken(request.getUsername());
            return ResponseEntity.ok(new AuthResponse(token, "Login successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    new AuthResponse(null, "Authentication failed: " + e.getMessage())
            );
        }
    }
}
