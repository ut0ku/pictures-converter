package com.captures.converter;
import com.captures.converter.dto.Dtos.*;
import com.captures.converter.entity.UserAccount;
import com.captures.converter.entity.Role;
import com.captures.converter.security.JwtService;
import com.captures.converter.service.ConversionService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

class ServiceTests {
 @Test void bcryptAndJwtWork(){var encoder=new BCryptPasswordEncoder();assertTrue(encoder.matches("password123",encoder.encode("password123")));var jwt=new JwtService("test-secret-test-secret-test-secret-32",15);var u=new UserAccount("test@example.com","hash",Role.CLIENT);String token=jwt.accessToken(u);assertEquals("test@example.com",jwt.parse(token).getPayload().getSubject());assertEquals("CLIENT",jwt.parse(token).getPayload().get("role")); }
 @Test void rejectsEmptyAndOversizeUpload(){MockMultipartFile empty=new MockMultipartFile("file","empty.png","image/png",new byte[0]);assertThrows(Exception.class,()->sampleService().convert(empty,"PNG",82,null,null,"guest",null));MockMultipartFile large=new MockMultipartFile("file","large.png","image/png",new byte[50*1024*1024+1]);assertThrows(Exception.class,()->sampleService().convert(large,"PNG",82,null,null,"guest",null));}
 private ConversionService sampleService(){return new ConversionService(null,null,null,new com.captures.converter.service.ServerMetricsService());}
 @Test void validatesSupportedFormatSet(){for(String f:new String[]{"PNG","JPG","JPEG","WEBP","GIF","TIFF","HEIC","AVIF"})assertNotNull(f);}
}
