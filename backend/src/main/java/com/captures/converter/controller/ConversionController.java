package com.captures.converter.controller;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.entity.*;
import com.captures.converter.repository.*;
import com.captures.converter.service.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;

@RestController @RequestMapping("/api")
public class ConversionController {
 private final ConversionService conversions;private final UserRepository users;private final GuestUsageRepository guestUsage;
 public ConversionController(ConversionService conversions,UserRepository users,GuestUsageRepository guestUsage){this.conversions=conversions;this.users=users;this.guestUsage=guestUsage;}
 @PostMapping(value="/convert",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ConversionResponse convert(@RequestParam("file")MultipartFile file,@RequestParam(defaultValue="PNG")String format,@RequestParam(required=false)Integer quality,@RequestParam(required=false)Integer width,@RequestParam(required=false)Integer height,@RequestHeader(value="X-Guest-Key",defaultValue="anonymous")String guestKey,Authentication authentication){
  String email=authentication==null?null:authentication.getName();return conversions.convert(file,format,quality,width,height,guestKey,email);
 }
 @GetMapping("/quota")
 public QuotaResponse quota(@RequestHeader(value="X-Guest-Key",defaultValue="anonymous")String guestKey,Authentication authentication){
  UserAccount u=authentication==null?null:users.findByEmail(authentication.getName()).orElse(null);GuestUsage g=u==null?guestUsage.findByGuestKeyAndUsageDate(guestKey,LocalDate.now()).orElse(null):null;return conversions.quota(u,g);
 }
 @GetMapping("/conversions")
 public Page<ConversionResponse> history(Authentication authentication,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int size){
  if(authentication==null)throw new com.captures.converter.exception.ApiException(HttpStatus.UNAUTHORIZED,"Требуется авторизация");
  UserAccount u=users.findByEmail(authentication.getName()).orElseThrow();return conversions.history(u,page,size);
 }
 @GetMapping("/conversions/{id}/download")
 public ResponseEntity<byte[]> download(@PathVariable Long id,@RequestHeader(value="X-Guest-Key",defaultValue="")String guestKey,Authentication authentication){
  Conversion c=conversions.getConversion(id);String ext=c.getResultFormat().equals("JPG")?"jpg":c.getResultFormat().toLowerCase();
  boolean isOwner=authentication!=null&&c.getOwner()!=null&&c.getOwner().getEmail().equalsIgnoreCase(authentication.getName());
  boolean isAdmin=authentication!=null&&authentication.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_ADMIN"));
  boolean isGuest=c.getOwner()==null&&c.getGuestKey()!=null&&c.getGuestKey().equals(guestKey);
  if(!isOwner&&!isAdmin&&!isGuest)throw new com.captures.converter.exception.ApiException(HttpStatus.FORBIDDEN,"Нет доступа к этой конвертации");
  String name=(c.getSourceName()==null?"image":c.getSourceName()).replaceAll("\\.[^.]+$","")+"."+ext;
  return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+name+"\"").contentType(MediaType.parseMediaType(c.getContentType())).body(c.getResultData());
 }
 @GetMapping("/admin/stats")
 public StatisticsResponse stats(){return conversions.stats();}
}
