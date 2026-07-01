package com.example.blog.taxonomy.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.blog.taxonomy.dto.TaxonomyItem;

@Mapper
public interface TaxonomyMapper {

    List<TaxonomyItem> findCategories();

    Optional<TaxonomyItem> findCategoryById(Long id);

    int insertCategory(
            @Param("name") String name,
            @Param("slug") String slug,
            @Param("description") String description,
            @Param("sortOrder") int sortOrder,
            @Param("visible") boolean visible
    );

    int updateCategory(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("slug") String slug,
            @Param("description") String description,
            @Param("sortOrder") int sortOrder,
            @Param("visible") boolean visible
    );

    int deleteCategory(Long id);

    List<TaxonomyItem> findTags();

    Optional<TaxonomyItem> findTagById(Long id);

    int insertTag(
            @Param("name") String name,
            @Param("slug") String slug,
            @Param("description") String description,
            @Param("sortOrder") int sortOrder,
            @Param("visible") boolean visible
    );

    int updateTag(
            @Param("id") Long id,
            @Param("name") String name,
            @Param("slug") String slug,
            @Param("description") String description,
            @Param("sortOrder") int sortOrder,
            @Param("visible") boolean visible
    );

    int deleteTag(Long id);

    Long lastInsertId();
}
