package com.captures.converter.service;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.entity.*;
import com.captures.converter.exception.ApiException;
import com.captures.converter.repository.*;
import com.captures.converter.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
public class AuthService {
 private final UserRepository users;private final RefreshTokenRepository refresh;private final PasswordEncoder encoder;private final JwtService jwt;
 private final long refreshDays;
 public AuthService(UserRepository users,RefreshTokenRepository refresh,PasswordEncoder encoder,JwtService jwt,@Value("${app.refresh-days}") long refreshDays){this.users=users;this.refresh=refresh;this.encoder=encoder;this.jwt=jwt;this.refreshDays=refreshDays;}
 public AuthResponse register(RegisterRequest r){
  String email=r.email().trim().toLowerCase();if(users.existsByEmail(email))throw new ApiException(HttpStatus.CONFLICT,"Пользователь уже существует");
  UserAccount u=users.save(new UserAccount(email,encoder.encode(r.password()),Role.CLIENT));return issue(u);
 }
 public AuthResponse login(LoginRequest r){UserAccount u=users.findByEmail(r.email().trim().toLowerCase()).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Неверный email или пароль"));if(!encoder.matches(r.password(),u.getPasswordHash()))throw new ApiException(HttpStatus.UNAUTHORIZED,"Неверный email или пароль");return issue(u);}
 private String random(){byte[] b=new byte[48];new SecureRandom().nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
 private String hash(String s){try{byte[] b=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));return Base64.getEncoder().encodeToString(b);}catch(Exception e){throw new IllegalStateException(e);}}
 private AuthResponse issue(UserAccount u){String rt=random();refresh.save(new RefreshToken(u,hash(rt),Instant.now().plusSeconds(refreshDays*86400)));return new AuthResponse(jwt.accessToken(u),rt,"Bearer",u.getEmail(),u.getRole().name());}
 public AuthResponse refresh(RefreshRequest r){RefreshToken t=refresh.findByTokenHashAndRevokedFalse(hash(r.refreshToken())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Refresh-токен недействителен"));if(t.getExpiresAt().isBefore(Instant.now()))throw new ApiException(HttpStatus.UNAUTHORIZED,"Refresh-токен истёк");t.setRevoked(true);refresh.save(t);return issue(t.getUser());}
 public MeResponse me(String email){UserAccount u=findUser(email);return response(u);}
 public MeResponse updateProfile(String email,UpdateProfileRequest request){
  UserAccount user=findUser(email);
  user.setDisplayName(request.displayName().trim());
  return response(users.save(user));
 }
 private UserAccount findUser(String email){return users.findByEmail(email).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Пользователь не найден"));}
 private MeResponse response(UserAccount user){return new MeResponse(user.getId(),user.getEmail(),user.getRole().name(),user.getDisplayName());}
}
