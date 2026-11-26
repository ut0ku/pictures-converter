package com.captures.converter.controller;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.exception.ApiException;
import com.captures.converter.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
 @PostMapping("/register") public AuthResponse register(@Valid @RequestBody RegisterRequest r){return auth.register(r);}
 @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest r){return auth.login(r);}
 @PostMapping("/refresh") public AuthResponse refresh(@Valid @RequestBody RefreshRequest r){return auth.refresh(r);}
 @GetMapping("/me") public MeResponse me(Authentication authentication){return auth.me(requireEmail(authentication));}
 @PatchMapping("/profile") public MeResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request,Authentication authentication){return auth.updateProfile(requireEmail(authentication),request);}
 private String requireEmail(Authentication authentication){
  if(authentication==null||!authentication.isAuthenticated()||"anonymousUser".equals(authentication.getName()))
   throw new ApiException(HttpStatus.UNAUTHORIZED,"Требуется авторизация");
  return authentication.getName();
 }
}
