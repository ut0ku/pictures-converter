package com.captures.converter.service;

import com.captures.converter.dto.Dtos.AdminUserResponse;
import com.captures.converter.dto.Dtos.GrantDailyBonusRequest;
import com.captures.converter.entity.Role;
import com.captures.converter.entity.UserAccount;
import com.captures.converter.exception.ApiException;
import com.captures.converter.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class AdminUserService {
 private static final long BYTES_PER_MEGABYTE=1024L*1024;
 private final UserRepository users;

 public AdminUserService(UserRepository users){this.users=users;}

 @Transactional(readOnly=true)
 public List<AdminUserResponse> listClients(){
  Instant now=Instant.now();
  return users.findByRoleOrderByEmailAsc(Role.CLIENT).stream()
    .map(user->response(user,now))
    .toList();
 }

 @Transactional
 public AdminUserResponse grantDailyBonus(Long userId,GrantDailyBonusRequest request){
  if(request.extraConversions()==0&&request.extraMegabytes()==0)
   throw new ApiException(HttpStatus.BAD_REQUEST,"Укажите хотя бы один дополнительный лимит");
  UserAccount user=users.findById(userId)
    .filter(account->account.getRole()==Role.CLIENT)
    .orElseThrow(()->new ApiException(HttpStatus.NOT_FOUND,"Пользователь не найден"));
  Instant now=Instant.now();
  try{
   long extraBytes=Math.multiplyExact(request.extraMegabytes(),BYTES_PER_MEGABYTE);
   user.grantDailyBonus(request.extraConversions(),extraBytes,now,now.plus(Duration.ofHours(24)));
   return response(users.save(user),now);
  }catch(ArithmeticException e){
   throw new ApiException(HttpStatus.BAD_REQUEST,"Превышено допустимое значение бонусного лимита");
  }
 }

 private AdminUserResponse response(UserAccount user,Instant now){
  return new AdminUserResponse(user.getId(),user.getEmail(),user.getDisplayName(),
    user.getActiveBonusDailyConversions(now),
    user.getActiveBonusDailyBytes(now)/BYTES_PER_MEGABYTE,
    user.getActiveBonusExpiresAt(now));
 }
}
