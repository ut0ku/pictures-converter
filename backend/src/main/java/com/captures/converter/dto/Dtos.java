package com.captures.converter.dto;
import jakarta.validation.constraints.*;
import java.util.*;
public class Dtos {
 public record RegisterRequest(@NotBlank @Email String email,@Size(min=8,max=72) String password){}
 public record LoginRequest(@NotBlank @Email String email,@NotBlank String password){}
 public record RefreshRequest(@NotBlank String refreshToken){}
 public record AuthResponse(String accessToken,String refreshToken,String tokenType,String email,String role){}
 public record UpdateProfileRequest(@NotBlank @Size(max=60) String displayName){}
 public record MeResponse(Long id,String email,String role,String displayName){}
 public record AdminUserResponse(Long id,String email,String displayName,long bonusDailyConversions,long bonusDailyMegabytes,java.time.Instant bonusExpiresAt){}
 public record GrantDailyBonusRequest(@Min(0) @Max(100000) long extraConversions,@Min(0) @Max(102400) long extraMegabytes){}
 public record ConversionRequest(@Pattern(regexp="(?i)PNG|JPG|JPEG|WEBP|GIF|TIFF|HEIC|AVIF") String format,@Min(1)@Max(100) Integer quality,@Min(1)@Max(10000) Integer width,@Min(1)@Max(10000) Integer height){}
 public record ConversionResponse(Long id,String sourceName,String sourceFormat,String resultFormat,long sourceSize,long resultSize,Date createdAt,String downloadUrl){}
 public record QuotaResponse(boolean authenticated,String tier,long dailyLimit,long dailyUsed,long totalLimitBytes,long totalUsedBytes,long bonusDailyConversions,long bonusDailyBytes,java.time.Instant bonusExpiresAt){}
 public record ErrorResponse(String error,String message,long timestamp){}
 public record StatisticsResponse(long conversions,long sourceBytes,long resultBytes,ServerMetrics server){}
 public record ServerMetrics(Double systemCpuLoad,Double processCpuLoad,long heapUsedBytes,long heapMaxBytes,
   Long systemMemoryUsedBytes,Long systemMemoryTotalBytes,long uptimeMillis,int threadCount,
   int availableProcessors,String javaVersion,String osName){}
}
