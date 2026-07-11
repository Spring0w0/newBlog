package com.spring0w0.backend;

import com.spring0w0.backend.mapper.UserMapper;
import com.spring0w0.backend.mapper.FileAssetMapper;
import com.spring0w0.backend.pojo.entity.User;
import com.spring0w0.backend.pojo.entity.FileAsset;
import com.spring0w0.backend.pojo.vo.AdminBlogSummaryVo;
import com.spring0w0.backend.pojo.vo.AdminBloggerVo;
import com.spring0w0.backend.pojo.vo.SiteSettingsVo;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import com.spring0w0.backend.pojo.vo.PageVo;
import com.spring0w0.backend.service.AboutService;
import com.spring0w0.backend.service.BlogService;
import com.spring0w0.backend.service.BloggerService;
import com.spring0w0.backend.service.JwtTokenService;
import com.spring0w0.backend.service.PictureService;
import com.spring0w0.backend.service.ProjectService;
import com.spring0w0.backend.service.ShareService;
import com.spring0w0.backend.service.SiteService;
import com.spring0w0.backend.service.SnippetService;
import com.spring0w0.backend.service.FileService;
import com.spring0w0.backend.config.UploadProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "app.security.jwt.secret=test-jwt-secret-must-contain-at-least-32-bytes",
        "app.security.jwt.access-token-expiration=PT1H",
        "app.security.jwt.issuer=newblog-test",
        "app.upload.root-dir=${java.io.tmpdir}/newblog-backend-test-uploads"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UploadProperties uploadProperties;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private FileAssetMapper fileAssetMapper;

    @MockitoBean
    private BlogService blogService;

    @MockitoBean
    private SiteService siteService;

    @MockitoBean
    private AboutService aboutService;

    @MockitoBean
    private BloggerService bloggerService;

    @MockitoBean
    private ProjectService projectService;

    @MockitoBean
    private ShareService shareService;

    @MockitoBean
    private PictureService pictureService;

    @MockitoBean
    private SnippetService snippetService;

    @MockitoBean
    private FileService fileService;

    @BeforeEach
    void setUp() {
        User admin = user("admin", "ADMIN");
        when(userMapper.selectOne(any())).thenReturn(admin);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void loginReturnsAccessTokenForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }

    @Test
    void loginRejectsInvalidPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"invalid\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void anonymousAdminRequestReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/example"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void userRoleIsForbiddenFromAdminEndpoint() throws Exception {
        User user = user("reader", "USER");
        when(userMapper.selectOne(any())).thenReturn(user);
        String token = jwtTokenService.createAccessToken("reader", "USER");

        mockMvc.perform(get("/api/admin/example").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    void malformedTokenReturnsJsonUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/example").header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void publishedBlogsAreAvailableWithoutAuthentication() throws Exception {
        when(blogService.getPublishedBlogs()).thenReturn(List.of(
                new BlogSummaryVo(
                        "public-post", "公开文章", List.of("测试"), "2026-07-11T12:00:00", "摘要", "/cover.png", false, "测试"
                )
        ));

        mockMvc.perform(get("/api/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].slug").value("public-post"))
                .andExpect(jsonPath("$.data[0].hidden").value(false));
    }

    @Test
    void administratorCanReadPaginatedBlogManagementList() throws Exception {
        when(blogService.getAdminBlogs(anyLong(), anyLong(), any())).thenReturn(new PageVo<>(
                List.of(new AdminBlogSummaryVo(
                        12L, "managed-post", "管理文章", List.of("测试"), "2026-07-11T12:00:00",
                        "摘要", "/cover.png", false, "测试"
                )),
                1, 20, 1, 1
        ));
        String token = jwtTokenService.createAccessToken("admin", "ADMIN");

        mockMvc.perform(get("/api/admin/blogs")
                        .param("page", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].id").value(12))
                .andExpect(jsonPath("$.data.items[0].slug").value("managed-post"));
    }

    @Test
    void administratorCanReadBloggerManagementList() throws Exception {
        when(bloggerService.getAdminBloggers()).thenReturn(List.of(
                new AdminBloggerVo(7L, "测试博主", "/images/bloggers/avatar.png", "https://example.com", "简介", 3, "recent")
        ));
        String token = jwtTokenService.createAccessToken("admin", "ADMIN");

        mockMvc.perform(get("/api/admin/bloggers").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(7))
                .andExpect(jsonPath("$.data[0].name").value("测试博主"));
    }

    @Test
    void administratorCanSaveSiteSettingsAtomically() throws Exception {
        var config = JsonNodeFactory.instance.objectNode();
        config.putObject("meta").put("title", "测试站点").put("description", "测试描述");
        var cardStyles = JsonNodeFactory.instance.objectNode();
        cardStyles.putObject("hiCard").put("width", 360).put("height", 288).put("order", 1).put("enabled", true);
        when(siteService.saveSiteSettings(any())).thenReturn(new SiteSettingsVo(config, cardStyles));
        String token = jwtTokenService.createAccessToken("admin", "ADMIN");

        mockMvc.perform(put("/api/admin/site/settings")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"config":{"meta":{"title":"测试站点","description":"测试描述"}},
                                 "cardStyles":{"hiCard":{"width":360,"height":288,"order":1,"enabled":true}}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.config.meta.title").value("测试站点"))
                .andExpect(jsonPath("$.data.cardStyles.hiCard.width").value(360));
    }

    @Test
    void expiredTokenReturnsJsonUnauthorized() throws Exception {
        String expiredToken = Jwts.builder()
                .subject("admin")
                .issuer("newblog-test")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor("test-jwt-secret-must-contain-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)))
                .compact();

        mockMvc.perform(get("/api/admin/example").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void corsPreflightAllowsConfiguredFrontendDevelopmentOrigin() throws Exception {
        mockMvc.perform(options("/api/auth/login")
                        .header(HttpHeaders.ORIGIN, "http://localhost:2025")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:2025"));
    }

    @Test
    void openApiDocumentDescribesImplementedEndpointsAndJwtScheme() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("NewBlog API"))
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.summary").value("管理员账号密码登录"))
                .andExpect(jsonPath("$.paths['/api/blogs/{slug}'].get.parameters[0].name").value("slug"))
                .andExpect(jsonPath("$.paths['/api/admin/files/images'].post.summary").value("上传图片"));
    }

    @Test
    void swaggerEntryUsesShortConfiguredPath() throws Exception {
        mockMvc.perform(get("/swagger"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string(HttpHeaders.LOCATION, org.hamcrest.Matchers.containsString("/swagger-ui/index.html")));
    }

    @Test
    void missingResourceReturnsUnifiedNotFoundResponse() throws Exception {
        mockMvc.perform(get("/not-registered-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("资源不存在"));
    }

    @Test
    void administratorCanUploadImageThroughSecuredEndpoint() throws Exception {
        FileAsset asset = new FileAsset();
        asset.setId(42L);
        asset.setScope("blog-images");
        asset.setStoredFilename("20260711-a1b2c3d4.png");
        asset.setRelativePath("blog-images/20260711-a1b2c3d4.png");
        asset.setOriginalName("cover.png");
        asset.setFileSize(67L);
        asset.setContentType("image/png");
        when(fileService.uploadImage(any(), org.mockito.ArgumentMatchers.eq("blog-images"))).thenReturn(asset);
        String token = jwtTokenService.createAccessToken("admin", "ADMIN");

        mockMvc.perform(multipart("/api/admin/files/images")
                        .file(new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1, 2, 3}))
                        .param("scope", "blog-images")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.fileId").value(42))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.containsString("/images/blog-images/20260711-a1b2c3d4.png")));
    }

    @Test
    void uploadedDirectoryIsMappedAsPublicReadOnlyImages() throws Exception {
        Path imageFile = uploadProperties.normalizedRootDir().resolve("blog-images/static-test.png");
        byte[] content = {1, 2, 3, 4};
        Files.createDirectories(imageFile.getParent());
        Files.write(imageFile, content);

        try {
            mockMvc.perform(get("/images/blog-images/static-test.png"))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(content));
        } finally {
            Files.deleteIfExists(imageFile);
        }
    }

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("admin"));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
