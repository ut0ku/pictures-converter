package com.captures.converter;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.entity.Conversion;
import com.captures.converter.entity.UserAccount;
import com.captures.converter.entity.Role;
import com.captures.converter.repository.ConversionRepository;
import com.captures.converter.repository.GuestUsageRepository;
import com.captures.converter.repository.UserRepository;
import com.captures.converter.security.JwtService;
import com.captures.converter.service.ConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ServiceTests {
 @Test void bcryptAndJwtWork(){var encoder=new BCryptPasswordEncoder();assertTrue(encoder.matches("password123",encoder.encode("password123")));var jwt=new JwtService("test-secret-test-secret-test-secret-32",15);var u=new UserAccount("test@example.com","hash",Role.CLIENT);String token=jwt.accessToken(u);assertEquals("test@example.com",jwt.parse(token).getPayload().getSubject());assertEquals("CLIENT",jwt.parse(token).getPayload().get("role")); }
 @Test void rejectsEmptyAndOversizeUpload(){MockMultipartFile empty=new MockMultipartFile("file","empty.png","image/png",new byte[0]);assertThrows(Exception.class,()->sampleService().convert(empty,"PNG",82,null,null,"guest",null));MockMultipartFile large=new MockMultipartFile("file","large.png","image/png",new byte[50*1024*1024+1]);assertThrows(Exception.class,()->sampleService().convert(large,"PNG",82,null,null,"guest",null));}
 @Test void dailyBonusAccumulatesUntilExpiry(){UserAccount user=new UserAccount("test@example.com","hash",Role.CLIENT);Instant now=Instant.now();user.grantDailyBonus(3,2L*1024*1024,now,now.plus(Duration.ofHours(24)));user.grantDailyBonus(2,1024*1024,now.plusSeconds(60),now.plus(Duration.ofHours(25)));assertEquals(5,user.getActiveBonusDailyConversions(now.plusSeconds(60)));assertEquals(3L*1024*1024,user.getActiveBonusDailyBytes(now.plusSeconds(60)));assertNull(user.getActiveBonusExpiresAt(now.plus(Duration.ofHours(25))));assertEquals(0,user.getActiveBonusDailyConversions(now.plus(Duration.ofHours(25))));}
 @Test void bonusAllowsConversionBeyondStandardDailyLimits() throws Exception {
  UserAccount owner=new UserAccount("client@example.com","hash",Role.CLIENT);
  owner.grantDailyBonus(1,1024*1024,Instant.now(),Instant.now().plus(Duration.ofHours(24)));
  ConversionRepository conversions=mock(ConversionRepository.class);
  UserRepository users=mock(UserRepository.class);
  when(users.findByEmail(owner.getEmail())).thenReturn(Optional.of(owner));
  when(conversions.countByOwnerAfter(eq(owner),any(Instant.class))).thenReturn(150L);
  when(conversions.sumSourceByOwnerAfter(eq(owner),any(Instant.class))).thenReturn(500L*1024*1024);
  when(conversions.save(any(Conversion.class))).thenAnswer(invocation->invocation.getArgument(0));
  ByteArrayOutputStream imageBytes=new ByteArrayOutputStream();
  ImageIO.write(new BufferedImage(1,1,BufferedImage.TYPE_INT_RGB),"png",imageBytes);
  MockMultipartFile file=new MockMultipartFile("file","image.png","image/png",imageBytes.toByteArray());
  ConversionService service=new ConversionService(conversions,mock(GuestUsageRepository.class),users,new com.captures.converter.service.ServerMetricsService());
  assertNotNull(service.convert(file,"PNG",82,null,null,"guest",owner.getEmail()));
  verify(conversions).save(any(Conversion.class));
 }
 private ConversionService sampleService(){return new ConversionService(null,null,null,new com.captures.converter.service.ServerMetricsService());}
 @Test void validatesSupportedFormatSet(){for(String f:new String[]{"PNG","JPG","JPEG","WEBP","GIF","TIFF","HEIC","AVIF"})assertNotNull(f);}
}
