package com.library.repository;

import com.library.entity.User;
import com.library.entity.enums.AccountStatus;
import com.library.entity.enums.SystemRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByAccountStatus(AccountStatus status);
    List<User> findBySystemRoleIn(java.util.Collection<SystemRole> roles);
    long countByAccountStatus(AccountStatus accountStatus);
    long countBySystemRole(SystemRole systemRole);
    long countBySystemRoleAndAccountStatus(SystemRole systemRole, AccountStatus accountStatus);
    long countBySystemRoleIn(java.util.Collection<SystemRole> roles);
    long countBySystemRoleInAndAccountStatus(java.util.Collection<SystemRole> roles, AccountStatus accountStatus);
}
