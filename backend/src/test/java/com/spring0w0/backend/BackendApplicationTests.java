package com.spring0w0.backend;

import com.spring0w0.backend.mapper.UserMapper;
import com.spring0w0.backend.pojo.entity.User;
import com.spring0w0.backend.pojo.vo.BlogSummaryVo;
import com.spring0w0.backend.service.AboutService;
import com.spring0w0.backend.service.BlogService;
import com.spring0w0.backend.service.BloggerService;
import com.spring0w0.backend.service.JwtTokenService;
import com.spring0w0.backend.service.PictureService;
import com.spring0w0.backend.service.ProjectService;
import com.spring0w0.backend.service.ShareService;
import com.spring0w0.backend.service.SiteService;
import com.spring0w0.backend.service.SnippetService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration",
        "app.security.jwt.secret=test-jwt-secret-must-contain-at-least-32-bytes",
        "app.security.jwt.access-token-expiration=PT1H",
        "app.security.jwt.issuer=newblog-test"
})
@AutoConfigureMockMvc
class BackendApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private UserMapper userMapper;

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
                .andExpect(jsonPath("$.paths['/api/blogs/{slug}'].get.parameters[0].name").value("slug"));
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

    private User user(String username, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("admin"));
        user.setRole(role);
        user.setEnabled(true);
        return user;
    }
}
