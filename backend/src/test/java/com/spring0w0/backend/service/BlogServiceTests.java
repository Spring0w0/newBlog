package com.spring0w0.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.BlogPostMapper;
import com.spring0w0.backend.mapper.CategoryMapper;
import com.spring0w0.backend.pojo.entity.BlogPost;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTests {

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Spy
    private JsonContentReader jsonContentReader = new JsonContentReader(new ObjectMapper());

    @InjectMocks
    private BlogService blogService;

    @Test
    void publishedBlogsAreMappedFromEntitiesReturnedByMapper() {
        BlogPost blogPost = new BlogPost();
        blogPost.setSlug("public-post");
        blogPost.setTitle("公开文章");
        blogPost.setTags("[\"测试\", \"Java\"]");
        blogPost.setSummary("摘要");
        blogPost.setCoverUrl("/cover.png");
        blogPost.setCategory("测试");
        blogPost.setHidden(false);
        blogPost.setPublishedAt(LocalDateTime.of(2026, 7, 11, 12, 0));
        when(blogPostMapper.selectList(any())).thenReturn(List.of(blogPost));

        var blogs = blogService.getPublishedBlogs();

        assertThat(blogs).singleElement().satisfies(blog -> {
            assertThat(blog.slug()).isEqualTo("public-post");
            assertThat(blog.tags()).containsExactly("测试", "Java");
            assertThat(blog.date()).isEqualTo("2026-07-11T12:00:00");
            assertThat(blog.hidden()).isFalse();
        });
    }
}
