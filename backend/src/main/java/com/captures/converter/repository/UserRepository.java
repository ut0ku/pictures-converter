package com.captures.converter.repository;
import com.captures.converter.entity.UserAccount;
import com.captures.converter.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface UserRepository extends JpaRepository<UserAccount,Long>{
 Optional<UserAccount> findByEmail(String email);
 boolean existsByEmail(String email);
 List<UserAccount> findByRoleOrderByEmailAsc(Role role);
}
