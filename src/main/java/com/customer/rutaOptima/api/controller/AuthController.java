package com.customer.rutaOptima.api.controller;

import com.customer.rutaOptima.api.dto.LoginRequest;
import com.customer.rutaOptima.api.dto.LoginResponse;
import com.customer.rutaOptima.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador de autenticación
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.authenticate(request.getEmail(), request.getPassword());
        
        LoginResponse response = LoginResponse.builder()
                .token(token)
                .email(request.getEmail())
                .rol("USER") // En producción, obtener del usuario
                .build();
        
        return ResponseEntity.ok(response);
    }
}
