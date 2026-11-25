package com.captures.converter.config;
import com.captures.converter.entity.*;
import com.captures.converter.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminSeeder implements ApplicationRunner {
 private final UserRepository users;private final PasswordEncoder encoder;
 private final String email,password;
 public AdminSeeder(UserRepository users,PasswordEncoder encoder,@Value("${app.admin-email:}")String email,@Value("${app.admin-password:}")String password){this.users=users;this.encoder=encoder;this.email=email;this.password=password;}
 @Override public void run(ApplicationArguments args){
  if(!email.isBlank()&&!password.isBlank()&&!users.existsByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)))
   users.save(new UserAccount(email.trim().toLowerCase(java.util.Locale.ROOT),encoder.encode(password),Role.ADMIN));
 }
}
