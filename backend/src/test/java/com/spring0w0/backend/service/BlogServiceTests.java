package com.spring0w0.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTests {

    @Mock
    private BlogPostMapper blogPostMapper;

    @Mock
    private BlogImageMapper blogImageMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private FileAssetMapper fileAssetMapper;

    @Spy
    private JsonContentReader jsonContentReader = new JsonContentReader(new ObjectMapper());

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Spy
    private UploadProperties uploadProperties = uploadProperties();

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

    @Test
    void creatingBlogLinksManagedCoverAndMarkdownImagesToFileAssets() {
        Category category = new Category();
        category.setName("后端");
        when(categoryMapper.selectOne(any())).thenReturn(category);

        FileAsset asset = new FileAsset();
        asset.setId(42L);
        asset.setScope("blog-images");
        asset.setRelativePath("blog-images/cover.png");
        when(fileAssetMapper.selectOne(any())).thenReturn(asset);
        when(blogImageMapper.delete(any())).thenReturn(0);
        when(blogImageMapper.insert((BlogImage) any())).thenReturn(1);
        when(blogImageMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            BlogPost blogPost = invocation.getArgument(0);
            blogPost.setId(24L);
            return 1;
        }).when(blogPostMapper).insert((BlogPost) any());

        BlogWriteRequest request = new BlogWriteRequest(
                "受管图片文章",
                "managed-image-post",
                "![正文图片](http://localhost:8080/images/blog-images/cover.png)",
                "摘要",
                List.of("Java", "Java"),
                "后端",
                "http://localhost:8080/images/blog-images/cover.png",
                false,
                LocalDateTime.of(2026, 7, 11, 12, 0)
        );

        var created = blogService.createBlog(request);

        ArgumentCaptor<BlogPost> blogCaptor = ArgumentCaptor.forClass(BlogPost.class);
        ArgumentCaptor<BlogImage> imageCaptor = ArgumentCaptor.forClass(BlogImage.class);
        verify(blogPostMapper).insert(blogCaptor.capture());
        verify(blogImageMapper).insert(imageCaptor.capture());
        assertThat(created.id()).isEqualTo(24L);
        assertThat(blogCaptor.getValue().getCoverFileAssetId()).isEqualTo(42L);
        assertThat(jsonContentReader.readStringList(blogCaptor.getValue().getTags())).containsExactly("Java");
        assertThat(imageCaptor.getValue().getBlogId()).isEqualTo(24L);
        assertThat(imageCaptor.getValue().getFileAssetId()).isEqualTo(42L);
        assertThat(imageCaptor.getValue().getUrl()).isEqualTo("/images/blog-images/cover.png");
    }

    @Test
    void savingCategoriesRejectsRemovalOfCategoryUsedByArticles() {
        Category category = new Category();
        category.setId(7L);
        category.setName("后端");
        category.setSortOrder(0);
        when(categoryMapper.selectList(any())).thenReturn(List.of(category));
        when(blogPostMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> blogService.saveCategories(new CategorySaveRequest(List.of())))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getResultCode())
                        .isEqualTo(ResultCode.UNPROCESSABLE_ENTITY))
                .hasMessageContaining("后端");
    }

    @Test
    void managedImageUrlsAreStoredAsRelativePathsAndReturnedAsConfiguredPublicUrls() {
        Category category = new Category();
        category.setName("后端");
        when(categoryMapper.selectOne(any())).thenReturn(category);

        FileAsset asset = new FileAsset();
        asset.setId(42L);
        asset.setScope("blog-images");
        asset.setRelativePath("blog-images/cover.png");
        when(fileAssetMapper.selectOne(any())).thenReturn(asset);
        when(blogImageMapper.delete(any())).thenReturn(0);
        when(blogImageMapper.insert((BlogImage) any())).thenReturn(1);
        when(blogImageMapper.selectList(any())).thenReturn(List.of());
        doAnswer(invocation -> {
            BlogPost blogPost = invocation.getArgument(0);
            blogPost.setId(25L);
            return 1;
        }).when(blogPostMapper).insert((BlogPost) any());

        var created = blogService.createBlog(new BlogWriteRequest(
                "图片地址规范化", "normalized-image-url", "![](http://localhost:8080/images/blog-images/cover.png)",
                null, List.of(), "后端", "http://localhost:8080/images/blog-images/cover.png", false, null
        ));

        ArgumentCaptor<BlogPost> blogCaptor = ArgumentCaptor.forClass(BlogPost.class);
        verify(blogPostMapper).insert(blogCaptor.capture());
        assertThat(blogCaptor.getValue().getCoverUrl()).isEqualTo("/images/blog-images/cover.png");
        assertThat(blogCaptor.getValue().getMarkdown()).contains("![](/images/blog-images/cover.png)");
        assertThat(created.cover()).isEqualTo("https://blog.example.com/images/blog-images/cover.png");
    }

    private static UploadProperties uploadProperties() {
        UploadProperties properties = new UploadProperties();
        properties.setPublicBaseUrl("https://blog.example.com");
        return properties;
    }
}
