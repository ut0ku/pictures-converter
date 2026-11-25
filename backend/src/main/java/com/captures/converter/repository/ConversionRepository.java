package com.captures.converter.repository;
import com.captures.converter.entity.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
public interface ConversionRepository extends JpaRepository<Conversion,Long>{
 Page<Conversion> findByOwnerOrderByCreatedAtDesc(UserAccount owner, Pageable pageable);
 @Query("select coalesce(sum(c.sourceSize),0) from Conversion c where c.owner=:owner and c.createdAt>=:from")
 long sumSourceByOwnerAfter(@Param("owner") UserAccount owner,@Param("from") Instant from);
 @Query("select count(c) from Conversion c where c.owner=:owner and c.createdAt>=:from")
 long countByOwnerAfter(@Param("owner") UserAccount owner,@Param("from") Instant from);
 @Query("select count(c) from Conversion c")
 long countAllConversions();
 @Query("select coalesce(sum(c.sourceSize),0) from Conversion c")
 long sumAllSourceBytes();
 @Query("select coalesce(sum(c.resultSize),0) from Conversion c")
 long sumAllResultBytes();
}
