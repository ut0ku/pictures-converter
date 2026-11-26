package com.captures.converter.repository;
import com.captures.converter.entity.GuestUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
public interface GuestUsageRepository extends JpaRepository<GuestUsage,Long>{
 Optional<GuestUsage> findByGuestKeyAndUsageDate(String guestKey,LocalDate usageDate);
}
