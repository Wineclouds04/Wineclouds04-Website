package com.example.blog.taxonomy.service;

import java.util.List;
import java.util.function.IntSupplier;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.shared.error.ApiException;
import com.example.blog.taxonomy.dto.TaxonomyItem;
import com.example.blog.taxonomy.dto.TaxonomyRequest;
import com.example.blog.taxonomy.mapper.TaxonomyMapper;

@Service
public class TaxonomyService {

    private final TaxonomyMapper taxonomyMapper;

    public TaxonomyService(TaxonomyMapper taxonomyMapper) {
        this.taxonomyMapper = taxonomyMapper;
    }

    public List<TaxonomyItem> findCategories() {
        return taxonomyMapper.findCategories();
    }

    public List<TaxonomyItem> findTags() {
        return taxonomyMapper.findTags();
    }

    @Transactional
    public TaxonomyItem createCategory(TaxonomyRequest request) {
        executeUnique(() -> taxonomyMapper.insertCategory(
                request.name().trim(),
                request.slug().trim(),
                blankToNull(request.description()),
                request.sortOrder(),
                request.visible()
        ));
        return require(taxonomyMapper.findCategoryById(taxonomyMapper.lastInsertId()), "分类");
    }

    @Transactional
    public TaxonomyItem updateCategory(Long id, TaxonomyRequest request) {
        executeUnique(() -> taxonomyMapper.updateCategory(
                id,
                request.name().trim(),
                request.slug().trim(),
                blankToNull(request.description()),
                request.sortOrder(),
                request.visible()
        ));
        return require(taxonomyMapper.findCategoryById(id), "分类");
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (taxonomyMapper.deleteCategory(id) == 0) {
            throw notFound("分类");
        }
    }

    @Transactional
    public TaxonomyItem createTag(TaxonomyRequest request) {
        executeUnique(() -> taxonomyMapper.insertTag(
                request.name().trim(),
                request.slug().trim(),
                blankToNull(request.description()),
                request.sortOrder(),
                request.visible()
        ));
        return require(taxonomyMapper.findTagById(taxonomyMapper.lastInsertId()), "标签");
    }

    @Transactional
    public TaxonomyItem updateTag(Long id, TaxonomyRequest request) {
        executeUnique(() -> taxonomyMapper.updateTag(
                id,
                request.name().trim(),
                request.slug().trim(),
                blankToNull(request.description()),
                request.sortOrder(),
                request.visible()
        ));
        return require(taxonomyMapper.findTagById(id), "标签");
    }

    @Transactional
    public void deleteTag(Long id) {
        if (taxonomyMapper.deleteTag(id) == 0) {
            throw notFound("标签");
        }
    }

    private void executeUnique(IntSupplier operation) {
        try {
            if (operation.getAsInt() == 0) {
                throw new ApiException(HttpStatus.NOT_FOUND, "分类或标签不存在");
            }
        } catch (DuplicateKeyException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "名称或 slug 已存在");
        }
    }

    private TaxonomyItem require(java.util.Optional<TaxonomyItem> item, String label) {
        return item.orElseThrow(() -> notFound(label));
    }

    private ApiException notFound(String label) {
        return new ApiException(HttpStatus.NOT_FOUND, label + "不存在");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
