package com.captures.converter.security;
import com.captures.converter.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean CorsConfigurationSource corsSource(@Value("${app.cors-origin}") String origins){
  CorsConfiguration c=new CorsConfiguration();
   List<String> allowedOrigins=new ArrayList<>(Arrays.stream(origins.split(","))
     .map(String::trim)
     .filter(s->!s.isBlank())
     .toList());
   allowedOrigins.addAll(List.of(
     "http://localhost:*",
     "http://127.0.0.1:*",
     "https://localhost:*",
     "https://127.0.0.1:*"
   ));
   c.setAllowedOriginPatterns(allowedOrigins);
  c.setAllowedMethods(List.of("GET","POST","OPTIONS","DELETE","PUT","PATCH"));
  c.setAllowedHeaders(List.of("*"));
  c.setAllowCredentials(true);
  UrlBasedCorsConfigurationSource s=new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**",c); return s;
 }
 @Bean SecurityFilterChain filter(HttpSecurity http,JwtService jwt,UserRepository users,CorsConfigurationSource corsSource) throws Exception{
  http.csrf(csrf->csrf.disable()).cors(c->c.configurationSource(corsSource)).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
  http.authorizeHttpRequests(a->a.requestMatchers("/api/auth/**","/api/convert","/api/quota","/swagger-ui/**","/swagger-ui.html","/v3/api-docs/**","/api/conversions/*/download").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").anyRequest().authenticated());
  http.addFilterBefore(new OncePerRequestFilter(){
   @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
    String h=req.getHeader("Authorization"); if(h!=null&&h.startsWith("Bearer "))try{
      String email=jwt.parse(h.substring(7)).getPayload().getSubject();
      users.findByEmail(email).ifPresent(u->SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,null,List.of(()->"ROLE_"+u.getRole().name()))));
    }catch(Exception ignored){}
    chain.doFilter(req,res);
   }
  },UsernamePasswordAuthenticationFilter.class);
  return http.build();
 }
}
