package com.cpt202.HerLink.mapper;

import java.util.List;
import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.cpt202.HerLink.entity.Category;

// Data access of category table
@Mapper
public interface CategoryMapper {

    List<Category> selectAllCategories();

    List<Category> selectActiveCategories();

    List<Category> selectByStatus(@Param("status") String status);

    Category selectById(@Param("categoryId") Long categoryId);

    Category selectActiveById(@Param("categoryId") Long categoryId);

    Category selectByTopic(@Param("categoryTopic") String categoryTopic);

    Integer countByTopicIgnoreCase(@Param("categoryTopic") String categoryTopic,
                                   @Param("excludedCategoryId") Long excludedCategoryId);

    int refreshUsageCount(@Param("categoryId") Long categoryId);

    int insert(Category category);

    int updateTopic(@Param("categoryId") Long categoryId,
                    @Param("categoryTopic") String categoryTopic,
                    @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);

    int updateStatus(@Param("categoryId") Long categoryId,
                     @Param("status") String status,
                     @Param("lastUpdatedAt") LocalDateTime lastUpdatedAt);
}
