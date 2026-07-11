package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.config.UploadProperties;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.BlogImageMapper;
import com.spring0w0.backend.mapper.BlogPostMapper;
import com.spring0w0.backend.mapper.CategoryMapper;
import com.spring0w0.backend.mapper.FileAssetMapper;
import com.spring0w0.backend.pojo.dto.BlogWriteRequest;
import com.spring0w0.backend.pojo.dto.CategorySaveRequest;
import com.spring0w0.backend.pojo.entity.BlogImage;
import com.spring0w0.backend.pojo.entity.BlogPost;
import com.spring0w0.backend.pojo.entity.Category;
import com.spring0w0.backend.pojo.entity.FileAsset;
import com.spring0w0.backend.pojo.vo.AdminBlogSummaryVo;
import com.spring0w0.backend.pojo.vo.AdminBlogVo;
import com.spring0w0.backend.pojo.vo.BlogDetailVo;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import com.spring0w0.backend.pojo.vo.PageVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章和文章分类的查询、管理服务。
 */
@Service
@RequiredArgsConstructor
public class BlogService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final String PUBLIC_IMAGE_PREFIX = "/images/";
    private static final String BLOG_IMAGE_PUBLIC_PREFIX = "/images/blog-images/";
    private static final Pattern MARKDOWN_IMAGE_URL_PATTERN = Pattern.compile("!\\[[^\\]]*]\\((?:<)?([^\\s)>]+)(?:>)?(?:\\s+[^)]*)?\\)");

    private final BlogPostMapper blogPostMapper;
    private final BlogImageMapper blogImageMapper;
    private final CategoryMapper categoryMapper;
    private final FileAssetMapper fileAssetMapper;
    private final JsonContentReader jsonContentReader;
    private final ObjectMapper objectMapper;
    private final UploadProperties uploadProperties;

    @Cacheable(cacheNames = "publishedBlogs", key = "'all'")
    public List<BlogSummaryVo> getPublishedBlogs() {
        return blogPostMapper.selectList(Wrappers.<BlogPost>lambdaQuery()
                        .eq(BlogPost::getHidden, false)
                        .orderByDesc(BlogPost::getPublishedAt)
                        .orderByDesc(BlogPost::getId))
                .stream()
                .map(this::toBlogSummary)
                .toList();
    }

    @Cacheable(cacheNames = "publishedBlog", key = "#slug")
    public Optional<BlogDetailVo> getPublishedBlog(String slug) {
        BlogPost blogPost = blogPostMapper.selectOne(Wrappers.<BlogPost>lambdaQuery()
                .eq(BlogPost::getSlug, slug)
                .eq(BlogPost::getHidden, false));
        return Optional.ofNullable(blogPost)
                .map(blog -> new BlogDetailVo(blog.getSlug(), blog.getMarkdown(), toBlogSummary(blog)));
    }

    @Cacheable(cacheNames = "blogCategories", key = "'all'")
    public List<String> getCategories() {
        return categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                        .orderByAsc(Category::getSortOrder)
                        .orderByAsc(Category::getId))
                .stream()
                .map(Category::getName)
                .toList();
    }

    public PageVo<AdminBlogSummaryVo> getAdminBlogs(long page, long pageSize, String slug) {
        LambdaQueryWrapper<BlogPost> query = Wrappers.<BlogPost>lambdaQuery()
                .orderByDesc(BlogPost::getPublishedAt)
                .orderByDesc(BlogPost::getId);
        String normalizedSlug = trimToNull(slug);
        if (normalizedSlug != null) {
            query.eq(BlogPost::getSlug, normalizedSlug);
        }

        Page<BlogPost> result = blogPostMapper.selectPage(Page.of(page, pageSize), query);
        return new PageVo<>(
                result.getRecords().stream().map(this::toAdminBlogSummary).toList(),
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages()
        );
    }

    public AdminBlogVo getAdminBlog(Long id) {
        return toAdminBlog(getRequiredBlog(id));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "publishedBlogs", allEntries = true),
            @CacheEvict(cacheNames = "publishedBlog", allEntries = true),
            @CacheEvict(cacheNames = "blogCategories", allEntries = true)
    })
    public AdminBlogVo createBlog(BlogWriteRequest request) {
        BlogPost blogPost = new BlogPost();
        applyBlogRequest(blogPost, request, LocalDateTime.now());
        if (blogPostMapper.insert(blogPost) != 1) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文章保存失败");
        }
        replaceBlogImages(blogPost.getId(), blogPost.getMarkdown());
        return toAdminBlog(blogPost);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "publishedBlogs", allEntries = true),
            @CacheEvict(cacheNames = "publishedBlog", allEntries = true),
            @CacheEvict(cacheNames = "blogCategories", allEntries = true)
    })
    public AdminBlogVo updateBlog(Long id, BlogWriteRequest request) {
        BlogPost blogPost = getRequiredBlog(id);
        applyBlogRequest(blogPost, request, blogPost.getPublishedAt());
        if (blogPostMapper.updateById(blogPost) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        replaceBlogImages(blogPost.getId(), blogPost.getMarkdown());
        return toAdminBlog(blogPost);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "publishedBlogs", allEntries = true),
            @CacheEvict(cacheNames = "publishedBlog", allEntries = true),
            @CacheEvict(cacheNames = "blogCategories", allEntries = true)
    })
    public void deleteBlog(Long id) {
        getRequiredBlog(id);
        blogImageMapper.delete(Wrappers.<BlogImage>lambdaQuery().eq(BlogImage::getBlogId, id));
        if (blogPostMapper.deleteById(id) != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "publishedBlogs", allEntries = true),
            @CacheEvict(cacheNames = "publishedBlog", allEntries = true),
            @CacheEvict(cacheNames = "blogCategories", allEntries = true)
    })
    public void deleteBlogs(List<Long> requestedIds) {
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(requestedIds));
        List<BlogPost> existingBlogs = blogPostMapper.selectByIds(ids);
        if (existingBlogs.size() != ids.size()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "待删除文章中存在不存在的记录");
        }
        blogImageMapper.delete(Wrappers.<BlogImage>lambdaQuery().in(BlogImage::getBlogId, ids));
        if (blogPostMapper.deleteByIds(ids) != ids.size()) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "批量删除文章失败");
        }
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "publishedBlogs", allEntries = true),
            @CacheEvict(cacheNames = "publishedBlog", allEntries = true),
            @CacheEvict(cacheNames = "blogCategories", allEntries = true)
    })
    public List<String> saveCategories(CategorySaveRequest request) {
        List<String> categoryNames = normalizeCategoryNames(request.categories());
        List<Category> existingCategories = categoryMapper.selectList(Wrappers.<Category>lambdaQuery()
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        Map<String, Category> existingByName = new LinkedHashMap<>();
        existingCategories.forEach(category -> existingByName.put(category.getName(), category));

        for (Category existingCategory : existingCategories) {
            if (!categoryNames.contains(existingCategory.getName())) {
                Long referenceCount = blogPostMapper.selectCount(Wrappers.<BlogPost>lambdaQuery()
                        .eq(BlogPost::getCategory, existingCategory.getName()));
                if (referenceCount != null && referenceCount > 0) {
                    throw new BusinessException(ResultCode.UNPROCESSABLE_ENTITY,
                            "分类“%s”仍被文章引用，无法删除".formatted(existingCategory.getName()));
                }
            }
        }

        List<Long> removableIds = existingCategories.stream()
                .filter(category -> !categoryNames.contains(category.getName()))
                .map(Category::getId)
                .toList();
        if (!removableIds.isEmpty()) {
            categoryMapper.deleteByIds(removableIds);
        }

        for (int index = 0; index < categoryNames.size(); index++) {
            String name = categoryNames.get(index);
            Category category = existingByName.get(name);
            if (category == null) {
                Category newCategory = new Category();
                newCategory.setName(name);
                newCategory.setSortOrder(index);
                if (categoryMapper.insert(newCategory) != 1) {
                    throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "分类保存失败");
                }
            } else if (!Integer.valueOf(index).equals(category.getSortOrder())) {
                category.setSortOrder(index);
                if (categoryMapper.updateById(category) != 1) {
                    throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "分类排序保存失败");
                }
            }
        }
        return categoryNames;
    }

    private void applyBlogRequest(BlogPost blogPost, BlogWriteRequest request, LocalDateTime defaultPublishedAt) {
        String category = validateCategory(request.category());
        String coverUrl = normalizeManagedBlogImageUrl(request.cover());
        String markdown = normalizeManagedBlogImageUrls(request.markdown());

        blogPost.setTitle(request.title().trim());
        blogPost.setSlug(request.slug().trim());
        blogPost.setMarkdown(markdown);
        blogPost.setSummary(trimToNull(request.summary()));
        blogPost.setTags(writeTags(normalizeTags(request.tags())));
        blogPost.setCategory(category);
        blogPost.setCoverUrl(coverUrl);
        blogPost.setCoverFileAssetId(resolveManagedBlogImageAssetId(coverUrl));
        blogPost.setHidden(Boolean.TRUE.equals(request.hidden()));
        blogPost.setPublishedAt(request.publishedAt() == null ? defaultPublishedAt : request.publishedAt());
    }

    private BlogPost getRequiredBlog(Long id) {
        BlogPost blogPost = blogPostMapper.selectById(id);
        if (blogPost == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文章不存在");
        }
        return blogPost;
    }

    private String validateCategory(String requestedCategory) {
        String category = trimToNull(requestedCategory);
        if (category == null) {
            return null;
        }
        Category existing = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery().eq(Category::getName, category));
        if (existing == null) {
            throw new BusinessException(ResultCode.UNPROCESSABLE_ENTITY, "文章分类不存在");
        }
        return category;
    }

    private void replaceBlogImages(Long blogId, String markdown) {
        blogImageMapper.delete(Wrappers.<BlogImage>lambdaQuery().eq(BlogImage::getBlogId, blogId));
        List<String> imageUrls = extractMarkdownImageUrls(markdown);
        for (int index = 0; index < imageUrls.size(); index++) {
            String url = imageUrls.get(index);
            BlogImage image = new BlogImage();
            image.setBlogId(blogId);
            image.setUrl(url);
            image.setFileAssetId(resolveManagedBlogImageAssetId(url));
            image.setSortOrder(index);
            if (blogImageMapper.insert(image) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "文章图片引用保存失败");
            }
        }
    }

    private List<String> extractMarkdownImageUrls(String markdown) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        Matcher matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(markdown);
        while (matcher.find()) {
            String url = trimToNull(matcher.group(1));
            if (url != null) {
                urls.add(url);
            }
        }
        return List.copyOf(urls);
    }

    private Long resolveManagedBlogImageAssetId(String url) {
        String relativePath = getManagedBlogImageRelativePath(url);
        if (relativePath == null) {
            return null;
        }
        FileAsset asset = fileAssetMapper.selectOne(Wrappers.<FileAsset>lambdaQuery()
                .eq(FileAsset::getRelativePath, relativePath));
        if (asset == null || !ImageUploadScope.BLOG_IMAGES.value().equals(asset.getScope())) {
            throw new BusinessException(ResultCode.UNPROCESSABLE_ENTITY, "文章引用的受管图片不存在或不属于文章图片范围");
        }
        return asset.getId();
    }

    private String getManagedBlogImageRelativePath(String url) {
        String publicPath = getManagedBlogImagePublicPath(url);
        return publicPath == null ? null : publicPath.substring(PUBLIC_IMAGE_PREFIX.length());
    }

    private String getManagedBlogImagePublicPath(String url) {
        String normalizedUrl = trimToNull(url);
        if (normalizedUrl == null) {
            return null;
        }
        try {
            URI uri = new URI(normalizedUrl);
            String path = uri.getPath();
            if (path == null || !path.startsWith(BLOG_IMAGE_PUBLIC_PREFIX)) {
                return null;
            }
            String relativePath = path.substring(PUBLIC_IMAGE_PREFIX.length());
            return relativePath.contains("/") && !relativePath.contains("..") ? path : null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String normalizeManagedBlogImageUrl(String url) {
        String normalizedUrl = trimToNull(url);
        String managedPublicPath = getManagedBlogImagePublicPath(normalizedUrl);
        return managedPublicPath == null ? normalizedUrl : managedPublicPath;
    }

    private String normalizeManagedBlogImageUrls(String markdown) {
        Matcher matcher = MARKDOWN_IMAGE_URL_PATTERN.matcher(markdown);
        StringBuilder normalizedMarkdown = new StringBuilder(markdown.length());
        int previousEnd = 0;
        while (matcher.find()) {
            normalizedMarkdown.append(markdown, previousEnd, matcher.start(1));
            normalizedMarkdown.append(normalizeManagedBlogImageUrl(matcher.group(1)));
            previousEnd = matcher.end(1);
        }
        if (previousEnd == 0) {
            return markdown;
        }
        normalizedMarkdown.append(markdown, previousEnd, markdown.length());
        return normalizedMarkdown.toString();
    }

    private String toPublicImageUrl(String url) {
        String managedPublicPath = getManagedBlogImagePublicPath(url);
        return managedPublicPath == null ? url : uploadProperties.buildPublicUrl(managedPublicPath);
    }

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalizedTag = trimToNull(tag);
            if (normalizedTag != null) {
                normalizedTags.add(normalizedTag);
            }
        }
        return List.copyOf(normalizedTags);
    }

    private List<String> normalizeCategoryNames(List<String> categories) {
        LinkedHashSet<String> normalizedCategories = new LinkedHashSet<>();
        for (String category : categories) {
            String normalizedCategory = trimToNull(category);
            if (normalizedCategory == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "分类名称不能为空");
            }
            if (!normalizedCategories.add(normalizedCategory)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "分类名称不能重复");
            }
        }
        return List.copyOf(normalizedCategories);
    }

    private String writeTags(List<String> tags) {
        try {
            return objectMapper.writeValueAsString(tags);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("文章标签无法序列化", exception);
        }
    }

    private BlogSummaryVo toBlogSummary(BlogPost blogPost) {
        return new BlogSummaryVo(
                blogPost.getSlug(),
                blogPost.getTitle(),
                jsonContentReader.readStringList(blogPost.getTags()),
                toDateString(blogPost.getPublishedAt()),
                blogPost.getSummary(),
                toPublicImageUrl(blogPost.getCoverUrl()),
                Boolean.TRUE.equals(blogPost.getHidden()),
                blogPost.getCategory()
        );
    }

    private AdminBlogSummaryVo toAdminBlogSummary(BlogPost blogPost) {
        return new AdminBlogSummaryVo(
                blogPost.getId(),
                blogPost.getSlug(),
                blogPost.getTitle(),
                jsonContentReader.readStringList(blogPost.getTags()),
                toDateString(blogPost.getPublishedAt()),
                blogPost.getSummary(),
                toPublicImageUrl(blogPost.getCoverUrl()),
                Boolean.TRUE.equals(blogPost.getHidden()),
                blogPost.getCategory()
        );
    }

    private AdminBlogVo toAdminBlog(BlogPost blogPost) {
        List<String> imageUrls = blogImageMapper.selectList(Wrappers.<BlogImage>lambdaQuery()
                        .eq(BlogImage::getBlogId, blogPost.getId())
                        .orderByAsc(BlogImage::getSortOrder)
                        .orderByAsc(BlogImage::getId))
                .stream()
                .map(BlogImage::getUrl)
                .map(this::toPublicImageUrl)
                .toList();
        return new AdminBlogVo(
                blogPost.getId(),
                blogPost.getSlug(),
                blogPost.getTitle(),
                blogPost.getMarkdown(),
                blogPost.getSummary(),
                jsonContentReader.readStringList(blogPost.getTags()),
                blogPost.getCategory(),
                toPublicImageUrl(blogPost.getCoverUrl()),
                Boolean.TRUE.equals(blogPost.getHidden()),
                toDateString(blogPost.getPublishedAt()),
                imageUrls
        );
    }

    private String toDateString(LocalDateTime dateTime) {
        return dateTime == null ? null : DATE_TIME_FORMATTER.format(dateTime);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
