package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.BlogPostMapper;
import com.spring0w0.backend.mapper.CategoryMapper;
import com.spring0w0.backend.pojo.entity.BlogPost;
import com.spring0w0.backend.pojo.entity.Category;
import com.spring0w0.backend.pojo.vo.BlogDetailVo;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * 公开文章和分类查询服务。
 */
@Service
@RequiredArgsConstructor
public class BlogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final BlogPostMapper blogPostMapper;
    private final CategoryMapper categoryMapper;
    private final JsonContentReader jsonContentReader;

    public List<BlogSummaryVo> getPublishedBlogs() {
        return blogPostMapper.selectList(Wrappers.<BlogPost>lambdaQuery()
                        .eq(BlogPost::getHidden, false)
                        .orderByDesc(BlogPost::getPublishedAt)
                        .orderByDesc(BlogPost::getId))
                .stream()
                .map(this::toBlogSummary)
                .toList();
    }

    public Optional<BlogDetailVo> getPublishedBlog(String slug) {
        BlogPost blogPost = blogPostMapper.selectOne(Wrappers.<BlogPost>lambdaQuery()
                .eq(BlogPost::getSlug, slug)
                .eq(BlogPost::getHidden, false));
        return Optional.ofNullable(blogPost)
                .map(blog -> new BlogDetailVo(blog.getSlug(), blog.getMarkdown(), toBlogSummary(blog)));
    }

    public List<String> getCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .map(Category::getName)
                .toList();
    }

    private BlogSummaryVo toBlogSummary(BlogPost blogPost) {
        return new BlogSummaryVo(
                blogPost.getSlug(),
                blogPost.getTitle(),
                jsonContentReader.readStringList(blogPost.getTags()),
                toDateString(blogPost.getPublishedAt()),
                blogPost.getSummary(),
                blogPost.getCoverUrl(),
                Boolean.TRUE.equals(blogPost.getHidden()),
                blogPost.getCategory()
        );
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }
}
