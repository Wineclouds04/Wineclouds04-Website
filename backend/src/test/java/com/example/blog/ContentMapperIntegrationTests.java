package com.example.blog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.blog.article.mapper.ArticleMapper;
import com.example.blog.media.mapper.MediaMapper;
import com.example.blog.taxonomy.mapper.TaxonomyMapper;

@SpringBootTest
@Transactional
class ContentMapperIntegrationTests {

    @Autowired
    private TaxonomyMapper taxonomyMapper;

    @Autowired
    private MediaMapper mediaMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Test
    void persistsAndReadsCategoryAndTag() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        taxonomyMapper.insertCategory("Category " + suffix, "category-" + suffix, null, 2, true);
        Long categoryId = taxonomyMapper.lastInsertId();
        taxonomyMapper.insertTag("Tag " + suffix, "tag-" + suffix, "description", 3, true);
        Long tagId = taxonomyMapper.lastInsertId();

        var category = taxonomyMapper.findCategoryById(categoryId).orElseThrow();
        var tag = taxonomyMapper.findTagById(tagId).orElseThrow();

        assertEquals(0, category.articleCount());
        assertEquals(0, tag.articleCount());
        assertTrue(category.visible());
        assertTrue(tag.visible());
    }

    @Test
    void persistsAndReadsMediaMetadata() {
        String objectKey = "blog/test/" + UUID.randomUUID() + ".png";
        mediaMapper.insert(
                objectKey,
                "test.png",
                "image/png",
                "png",
                128,
                16,
                8,
                "0".repeat(64),
                "test image",
                null
        );
        Long id = mediaMapper.lastInsertId();

        var media = mediaMapper.findById(id).orElseThrow();

        assertEquals(objectKey, media.objectKey());
        assertEquals(0, media.referenceCount());
        assertEquals("PENDING", media.status());
    }

    @Test
    void persistsAndReadsArticleRecordAndListProjection() {
        String slug = "integration-" + UUID.randomUUID().toString().substring(0, 8);
        articleMapper.insert(
                "Integration article",
                slug,
                null,
                "# Integration",
                "<h1>Integration</h1>",
                "Integration",
                null,
                "PUBLIC",
                false,
                true,
                1,
                1,
                null,
                null,
                null
        );
        Long id = articleMapper.lastInsertId();

        var detail = articleMapper.findById(id).orElseThrow();
        var page = articleMapper.findPage("DRAFT", "Integration article", 0, 20);

        assertEquals(slug, detail.slug());
        assertTrue(page.stream().anyMatch(item -> item.id().equals(id)));
    }
}
