package com.example.taskapi.auth;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthModels.TokenResponse register(@Valid @RequestBody AuthModels.RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthModels.TokenResponse login(@Valid @RequestBody AuthModels.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthModels.TokenResponse refresh(@Valid @RequestBody AuthModels.RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody AuthModels.RefreshRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public AuthModels.UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.profile(Long.valueOf(jwt.getSubject()));
    }
}