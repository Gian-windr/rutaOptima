package com.customer.rutaOptima.service;

import com.customer.rutaOptima.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Servicio de autenticación simplificado
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtUtil jwtUtil;

    public String authenticate(String email, String password) {
        // Autenticación hardcodeada para demo
        // En producción: validar contra base de datos
        if ("admin@rutaoptima.com".equals(email) && "password".equals(password)) {
            UserDetails userDetails = User.builder()
                    .username(email)
                    .password(password)
                    .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")))
                    .build();
            return jwtUtil.generateToken(userDetails);
        }
        
        throw new RuntimeException("Credenciales inválidas");
    }
}
