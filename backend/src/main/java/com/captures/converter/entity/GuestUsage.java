package com.captures.converter.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity @Table(name="guest_usage", uniqueConstraints=@UniqueConstraint(columnNames={"guestKey","usageDate"}))
public class GuestUsage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false) private String guestKey;
 @Column(nullable=false) private LocalDate usageDate;
 @Column(nullable=false) private int count;
 public GuestUsage(){}
 public GuestUsage(String guestKey,LocalDate usageDate){this.guestKey=guestKey;this.usageDate=usageDate;}
 public String getGuestKey(){return guestKey;} public LocalDate getUsageDate(){return usageDate;} public int getCount(){return count;}
 public void increment(){count++;}
}
