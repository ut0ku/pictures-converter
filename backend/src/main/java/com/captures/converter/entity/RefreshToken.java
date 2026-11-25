package com.captures.converter.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="refresh_tokens")
public class RefreshToken {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(optional=false) private UserAccount user;
 @Column(unique=true,nullable=false) private String tokenHash;
 @Column(nullable=false) private Instant expiresAt;
 private boolean revoked;
 public RefreshToken(){}
 public RefreshToken(UserAccount user,String tokenHash,Instant expiresAt){this.user=user;this.tokenHash=tokenHash;this.expiresAt=expiresAt;}
 public UserAccount getUser(){return user;} public Instant getExpiresAt(){return expiresAt;} public boolean isRevoked(){return revoked;} public void setRevoked(boolean revoked){this.revoked=revoked;}
}
