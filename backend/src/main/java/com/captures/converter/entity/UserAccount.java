package com.captures.converter.entity;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="users")
public class UserAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(unique=true,nullable=false) private String email;
 @Column(length=60) private String displayName;
 @Column(nullable=false) private String passwordHash;
 @Enumerated(EnumType.STRING) @Column(nullable=false) private Role role=Role.CLIENT;
 @Column(nullable=false) private Instant createdAt=Instant.now();
 public UserAccount(){}
 public UserAccount(String email,String passwordHash,Role role){this.email=email;this.passwordHash=passwordHash;this.role=role;}
 public Long getId(){return id;} public String getEmail(){return email;} public String getDisplayName(){return displayName;}
 public void setDisplayName(String displayName){this.displayName=displayName;}
 public String getPasswordHash(){return passwordHash;}
 public Role getRole(){return role;} public Instant getCreatedAt(){return createdAt;}
}
