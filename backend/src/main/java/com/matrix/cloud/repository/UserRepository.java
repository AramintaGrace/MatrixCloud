package com.matrix.cloud.repository;

import com.matrix.cloud.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByNicknameContaining(String nickname);
    List<User> findByNicknameContainingOrEmailContaining(String nickname, String email);
    List<User> findByRole(User.Role role);
    long countByStatus(User.Status status);
}
