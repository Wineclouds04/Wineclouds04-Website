package com.example.blog.shared.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

import com.example.blog.article.dto.ArticleUpsertRequest;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class GlobalExceptionHandlerTests {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void reportsMalformedRequestFieldInsteadOfGenericFrameworkMessage() throws Exception {
        JsonMappingException mappingException;
        try {
            new ObjectMapper().readValue("""
                    {
                      "title": "Test",
                      "slug": "test",
                      "summary": "",
                      "contentMarkdown": "",
                      "categoryId": null,
                      "tagIds": [],
                      "visibility": "PUBLIC",
                      "pinned": false,
                      "allowComment": true,
                      "metaTitle": "",
                      "metaDescription": "",
                      "canonicalUrl": "",
                      "version": "not-a-number"
                    }
                    """, ArticleUpsertRequest.class);
            throw new AssertionError("Expected malformed version to fail");
        } catch (JsonMappingException exception) {
            mappingException = exception;
        }
        var unreadable = new HttpMessageNotReadableException(
                "Invalid request content.",
                mappingException,
                new MockHttpInputMessage(new byte[0])
        );
        var request = new MockHttpServletRequest("POST", "/api/v1/admin/articles");

        var problem = handler.handleUnreadableRequest(unreadable, request);

        assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
        assertEquals("version: 字段格式不正确", problem.getDetail());
    }
}
