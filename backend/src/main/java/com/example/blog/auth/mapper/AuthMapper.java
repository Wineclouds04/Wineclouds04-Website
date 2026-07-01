package com.example.blog.auth.mapper;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.auth.model.UserAccount;

@Mapper
public interface AuthMapper {

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findById(Long id);

    long countUsers();

    int insertAdmin(
            @Param("username") String username,
            @Param("passwordHash") String passwordHash,
            @Param("nickname") String nickname
    );

    int updateLastLogin(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    int updatePassword(
            @Param("id") Long id,
            @Param("passwordHash") String passwordHash,
            @Param("changedAt") LocalDateTime changedAt
    );
}
