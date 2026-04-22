package com.cpt202.HerLink.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.AppUser;

@Mapper
public interface AppUserMapper {

    int insert(AppUser user);

    AppUser selectById(@Param("userId") Long userId);

    AppUser selectByEmail(@Param("email") String email);

    AppUser selectByUsername(@Param("username") String username);

    int updateBasicInfo(AppUser user);

    int updateContributorFlag(@Param("userId") Long userId,
                              @Param("contributor") boolean contributor,
                              @Param("updatedAt") java.time.LocalDateTime updatedAt);
}
