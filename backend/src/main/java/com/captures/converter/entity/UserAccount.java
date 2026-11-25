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
 private Long bonusDailyConversions=0L;
 private Long bonusDailyBytes=0L;
 private Instant bonusExpiresAt;
 public UserAccount(){}
 public UserAccount(String email,String passwordHash,Role role){this.email=email;this.passwordHash=passwordHash;this.role=role;}
 public Long getId(){return id;} public String getEmail(){return email;} public String getDisplayName(){return displayName;}
 public void setDisplayName(String displayName){this.displayName=displayName;}
 public String getPasswordHash(){return passwordHash;}
 public Role getRole(){return role;} public Instant getCreatedAt(){return createdAt;}
 public long getActiveBonusDailyConversions(Instant now){return isBonusActive(now)?bonusDailyConversions():0;}
 public long getActiveBonusDailyBytes(Instant now){return isBonusActive(now)?bonusDailyBytes():0;}
 public Instant getActiveBonusExpiresAt(Instant now){return isBonusActive(now)?bonusExpiresAt:null;}
 public void grantDailyBonus(long conversions,long bytes,Instant now,Instant expiresAt){
  if(isBonusActive(now)){
   bonusDailyConversions=Math.addExact(bonusDailyConversions(),conversions);
   bonusDailyBytes=Math.addExact(bonusDailyBytes(),bytes);
  }else{
   bonusDailyConversions=conversions;
   bonusDailyBytes=bytes;
  }
  bonusExpiresAt=expiresAt;
 }
 private boolean isBonusActive(Instant now){return bonusExpiresAt!=null&&bonusExpiresAt.isAfter(now);}
 private long bonusDailyConversions(){return bonusDailyConversions==null?0:bonusDailyConversions;}
 private long bonusDailyBytes(){return bonusDailyBytes==null?0:bonusDailyBytes;}
}
