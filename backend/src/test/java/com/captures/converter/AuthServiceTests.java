package com.captures.converter;

import com.captures.converter.dto.Dtos.UpdateProfileRequest;
import com.captures.converter.entity.Role;
import com.captures.converter.entity.UserAccount;
import com.captures.converter.repository.RefreshTokenRepository;
import com.captures.converter.repository.UserRepository;
import com.captures.converter.security.JwtService;
import com.captures.converter.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTests {
 @Test void updatesAndReturnsOnlyTheCurrentUsersDisplayName(){
  UserRepository users=mock(UserRepository.class);
  UserAccount account=new UserAccount("client@example.com","hash",Role.CLIENT);
  when(users.findByEmail("client@example.com")).thenReturn(Optional.of(account));
  when(users.save(any(UserAccount.class))).thenAnswer(invocation->invocation.getArgument(0));
  AuthService service=new AuthService(users,mock(RefreshTokenRepository.class),mock(PasswordEncoder.class),
    new JwtService("test-secret-test-secret-test-secret-32",15),30);

  var profile=service.updateProfile("client@example.com",new UpdateProfileRequest("  Alex Smith  "));

  assertEquals("Alex Smith",profile.displayName());
  assertEquals("client@example.com",profile.email());
  assertEquals("CLIENT",profile.role());
 }
}
