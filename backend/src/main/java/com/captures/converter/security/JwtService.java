package com.captures.converter.security;
import com.captures.converter.entity.UserAccount;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
 private final SecretKey key; private final long accessMs;
 public JwtService(@Value("${app.jwt-secret}") String secret,@Value("${app.access-minutes}") long accessMinutes){
  this.key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); this.accessMs=accessMinutes*60000L;
 }
 public String accessToken(UserAccount u){
  return Jwts.builder().subject(u.getEmail()).claim("role",u.getRole().name()).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+accessMs)).signWith(key).compact();
 }
 public Jws<Claims> parse(String token){return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);}
}
