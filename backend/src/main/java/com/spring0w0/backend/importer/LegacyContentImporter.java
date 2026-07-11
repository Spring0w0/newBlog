package com.spring0w0.backend.importer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.Map;

/**
 * 将原 frontend 目录中的静态内容一次性导入到当前数据库。
 * 该类不注册为 Spring Bean，只能通过显式开启的导入配置运行。
 */
@Slf4j
@RequiredArgsConstructor
final class LegacyContentImporter {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    void importAll(Path frontendRoot) {
        requireDirectory(frontendRoot);

        JsonNode siteContent = readJson(frontendRoot.resolve("src/config/site-content.json"));
        JsonNode cardStyles = readJson(frontendRoot.resolve("src/config/card-styles.json"));
        importSiteConfig(siteContent);
        importCardStyles(cardStyles);
        importBlogs(frontendRoot.resolve("public/blogs"));
        importCategories(readJson(frontendRoot.resolve("public/blogs/categories.json")));
        importAbout(readJson(frontendRoot.resolve("src/app/about/list.json")));
        importBloggers(readJson(frontendRoot.resolve("src/app/bloggers/list.json")));
        importProjects(readJson(frontendRoot.resolve("src/app/projects/list.json")));
        importShares(readJson(frontendRoot.resolve("src/app/share/list.json")));
        importPictures(readJson(frontendRoot.resolve("src/app/pictures/list.json")));
        importSnippets(readJson(frontendRoot.resolve("src/app/snippets/list.json")));
    }

    private void importSiteConfig(JsonNode siteContent) {
        JsonNode meta = siteContent.path("meta");
        JsonNode beian = siteContent.path("beian");
        jdbcTemplate.update(
                """
                        UPDATE site_config
                        SET meta_title = ?, meta_description = ?, beian = ?, theme = ?
                        WHERE id = 1
                        """,
                text(meta, "title"),
                text(meta, "description"),
                text(beian, "text"),
                json(siteContent)
        );

        jdbcTemplate.update("DELETE FROM art_images");
        int artOrder = 0;
        for (JsonNode artImage : array(siteContent, "artImages")) {
            jdbcTemplate.update(
                    "INSERT INTO art_images (id, url, description, sort_order) VALUES (?, ?, ?, ?)",
                    text(artImage, "id"),
                    text(artImage, "url"),
                    text(artImage, "description"),
                    artOrder++
            );
        }

        jdbcTemplate.update("DELETE FROM background_images");
        int backgroundOrder = 0;
        for (JsonNode backgroundImage : array(siteContent, "backgroundImages")) {
            String id = text(backgroundImage, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            jdbcTemplate.update(
                    "INSERT INTO background_images (id, url, sort_order) VALUES (?, ?, ?)",
                    id,
                    text(backgroundImage, "url"),
                    backgroundOrder++
            );
        }

        jdbcTemplate.update("DELETE FROM social_buttons");
        int socialOrder = 0;
        for (JsonNode socialButton : array(siteContent, "socialButtons")) {
            jdbcTemplate.update(
                    "INSERT INTO social_buttons (id, type, value, label, sort_order) VALUES (?, ?, ?, ?, ?)",
                    text(socialButton, "id"),
                    text(socialButton, "type"),
                    text(socialButton, "value"),
                    text(socialButton, "label"),
                    number(socialButton, "order", socialOrder++)
            );
        }
    }

    private void importCardStyles(JsonNode cardStyles) {
        jdbcTemplate.update("DELETE FROM card_styles");
        Iterator<Map.Entry<String, JsonNode>> fields = cardStyles.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode style = entry.getValue();
            jdbcTemplate.update(
                    """
                            INSERT INTO card_styles (card_key, width, height, offset_x, offset_y, sort_order, enabled, config)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    entry.getKey(),
                    nullableNumber(style, "width"),
                    nullableNumber(style, "height"),
                    nullableNumber(style, "offsetX"),
                    nullableNumber(style, "offsetY"),
                    number(style, "order", 0),
                    bool(style, "enabled", true),
                    json(style)
            );
        }
    }

    private void importBlogs(Path blogsRoot) {
        JsonNode index = readJson(blogsRoot.resolve("index.json"));
        jdbcTemplate.update("DELETE FROM blog_posts");

        int imported = 0;
        for (JsonNode indexItem : index) {
            String slug = text(indexItem, "slug");
            if (slug == null || slug.isBlank()) {
                continue;
            }

            Path blogDirectory = blogsRoot.resolve(slug);
            Path configPath = blogDirectory.resolve("config.json");
            Path markdownPath = blogDirectory.resolve("index.md");
            if (!Files.isRegularFile(markdownPath)) {
                log.warn("跳过历史文章导入，参数：slug={}，原因：Markdown 文件不存在，path={}", slug, markdownPath);
                continue;
            }

            JsonNode config = Files.isRegularFile(configPath) ? readJson(configPath) : indexItem;
            String title = firstText(config, indexItem, "title");
            String date = firstText(config, indexItem, "date");
            jdbcTemplate.update(
                    """
                            INSERT INTO blog_posts (slug, title, markdown, summary, tags, category, cover_url, hidden, published_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    slug,
                    title == null || title.isBlank() ? slug : title,
                    readText(markdownPath),
                    firstText(config, indexItem, "summary"),
                    json(firstArray(config, indexItem, "tags")),
                    firstText(config, indexItem, "category"),
                    firstText(config, indexItem, "cover"),
                    firstBoolean(config, indexItem, "hidden", false),
                    toTimestamp(date)
            );
            imported++;
        }
        log.info("历史文章导入完成，返回参数：导入数量={}", imported);
    }

    private void importCategories(JsonNode categories) {
        jdbcTemplate.update("DELETE FROM categories");
        int sortOrder = 0;
        for (JsonNode category : categories.path("categories")) {
            if (category.isTextual() && !category.asText().isBlank()) {
                jdbcTemplate.update(
                        "INSERT INTO categories (name, sort_order) VALUES (?, ?)",
                        category.asText(),
                        sortOrder++
                );
            }
        }
    }

    private void importAbout(JsonNode about) {
        jdbcTemplate.update(
                "UPDATE about SET title = ?, description = ?, content = ? WHERE id = 1",
                text(about, "title"),
                text(about, "description"),
                text(about, "content")
        );
    }

    private void importBloggers(JsonNode bloggers) {
        jdbcTemplate.update("DELETE FROM bloggers");
        int sortOrder = 0;
        for (JsonNode blogger : bloggers) {
            jdbcTemplate.update(
                    """
                            INSERT INTO bloggers (name, avatar_url, url, description, stars, status, sort_order)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    text(blogger, "name"),
                    text(blogger, "avatar"),
                    text(blogger, "url"),
                    text(blogger, "description"),
                    number(blogger, "stars", 3),
                    text(blogger, "status"),
                    sortOrder++
            );
        }
    }

    private void importProjects(JsonNode projects) {
        jdbcTemplate.update("DELETE FROM projects");
        int sortOrder = 0;
        for (JsonNode project : projects) {
            jdbcTemplate.update(
                    """
                            INSERT INTO projects (name, project_year, description, url, image_url, tags, github_url, npm_url, sort_order)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    text(project, "name"),
                    nullableNumber(project, "year"),
                    text(project, "description"),
                    text(project, "url"),
                    text(project, "image"),
                    json(array(project, "tags")),
                    text(project, "github"),
                    text(project, "npm"),
                    sortOrder++
            );
        }
    }

    private void importShares(JsonNode shares) {
        jdbcTemplate.update("DELETE FROM shares");
        int sortOrder = 0;
        for (JsonNode share : shares) {
            jdbcTemplate.update(
                    "INSERT INTO shares (name, url, logo_url, description, tags, stars, sort_order) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    text(share, "name"),
                    text(share, "url"),
                    text(share, "logo"),
                    text(share, "description"),
                    json(array(share, "tags")),
                    number(share, "stars", 3),
                    sortOrder++
            );
        }
    }

    private void importPictures(JsonNode pictures) {
        jdbcTemplate.update("DELETE FROM pictures");
        int sortOrder = 0;
        for (JsonNode picture : pictures) {
            ArrayNode images = array(picture, "images");
            if (images.size() == 0 && picture.hasNonNull("image")) {
                images.add(picture.get("image").asText());
            }
            jdbcTemplate.update(
                    "INSERT INTO pictures (id, images, description, sort_order, uploaded_at) VALUES (?, ?, ?, ?, ?)",
                    text(picture, "id"),
                    json(images),
                    text(picture, "description"),
                    sortOrder++,
                    toTimestamp(text(picture, "uploadedAt"))
            );
        }
    }

    private void importSnippets(JsonNode snippets) {
        jdbcTemplate.update("DELETE FROM snippets");
        int sortOrder = 0;
        for (JsonNode snippet : snippets) {
            if (snippet.isTextual() && !snippet.asText().isBlank()) {
                jdbcTemplate.update(
                        "INSERT INTO snippets (content, sort_order) VALUES (?, ?)",
                        snippet.asText(),
                        sortOrder++
                );
            }
        }
    }

    private JsonNode readJson(Path path) {
        try {
            return objectMapper.readTree(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取旧内容文件: " + path, exception);
        }
    }

    private String readText(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("无法读取旧文章内容: " + path, exception);
        }
    }

    private String json(JsonNode value) {
        try {
            return objectMapper.writeValueAsString(value == null ? objectMapper.createObjectNode() : value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化旧内容 JSON", exception);
        }
    }

    private ArrayNode array(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isArray() ? (ArrayNode) value : objectMapper.createArrayNode();
    }

    private JsonNode firstArray(JsonNode primary, JsonNode fallback, String fieldName) {
        return primary.path(fieldName).isArray() ? primary.path(fieldName) : array(fallback, fieldName);
    }

    private String firstText(JsonNode primary, JsonNode fallback, String fieldName) {
        String primaryValue = text(primary, fieldName);
        return primaryValue == null ? text(fallback, fieldName) : primaryValue;
    }

    private boolean firstBoolean(JsonNode primary, JsonNode fallback, String fieldName, boolean defaultValue) {
        if (primary.has(fieldName)) {
            return primary.path(fieldName).asBoolean(defaultValue);
        }
        return fallback.path(fieldName).asBoolean(defaultValue);
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText() : null;
    }

    private int number(JsonNode node, String fieldName, int defaultValue) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asInt() : defaultValue;
    }

    private Integer nullableNumber(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isNumber() ? value.asInt() : null;
    }

    private boolean bool(JsonNode node, String fieldName, boolean defaultValue) {
        JsonNode value = node.path(fieldName);
        return value.isBoolean() ? value.asBoolean() : defaultValue;
    }

    private Timestamp toTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.endsWith("Z")) {
                return Timestamp.from(Instant.parse(value));
            }
            return Timestamp.valueOf(LocalDateTime.parse(value));
        } catch (DateTimeParseException ignored) {
            try {
                return Timestamp.valueOf(LocalDate.parse(value).atStartOfDay());
            } catch (DateTimeParseException exception) {
                throw new IllegalStateException("无法解析旧内容日期: " + value, exception);
            }
        }
    }

    private void requireDirectory(Path path) {
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("旧内容目录不存在: " + path);
        }
    }
}
