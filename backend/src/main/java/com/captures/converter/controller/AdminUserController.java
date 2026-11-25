package com.captures.converter.controller;

import com.captures.converter.dto.Dtos.AdminUserResponse;
import com.captures.converter.dto.Dtos.GrantDailyBonusRequest;
import com.captures.converter.service.AdminUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
 private final AdminUserService adminUsers;

 public AdminUserController(AdminUserService adminUsers){this.adminUsers=adminUsers;}

 @GetMapping
 public List<AdminUserResponse> listClients(){return adminUsers.listClients();}

 @PostMapping("/{userId}/bonus")
 public AdminUserResponse grantDailyBonus(@PathVariable Long userId,@Valid @RequestBody GrantDailyBonusRequest request){
  return adminUsers.grantDailyBonus(userId,request);
 }
}
