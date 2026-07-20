package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.ArtImageMapper;
import com.spring0w0.backend.mapper.BackgroundImageMapper;
import com.spring0w0.backend.mapper.CardStyleMapper;
import com.spring0w0.backend.mapper.SiteConfigMapper;
import com.spring0w0.backend.mapper.SocialButtonMapper;
import com.spring0w0.backend.pojo.dto.SiteSettingsSaveRequest;
import com.spring0w0.backend.pojo.entity.ArtImage;
import com.spring0w0.backend.pojo.entity.BackgroundImage;
import com.spring0w0.backend.pojo.entity.CardStyle;
import com.spring0w0.backend.pojo.entity.SiteConfig;
import com.spring0w0.backend.pojo.entity.SocialButton;
import com.spring0w0.backend.pojo.vo.SiteSettingsVo;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** 站点配置、首页卡片样式及其图片引用的查询与保存服务。 */
@Service
@RequiredArgsConstructor
public class SiteService {

    private static final Pattern COLOR_PATTERN = Pattern.compile("#[0-9a-fA-F]{3,4}|#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8}");
    private static final Pattern RESOURCE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,80}");
    private static final Pattern CARD_KEY_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9_-]{0,79}");
    private static final int MAX_IMAGE_ITEMS = 50;
    private static final int MAX_SOCIAL_BUTTONS = 30;

    private final SiteConfigMapper siteConfigMapper;
    private final CardStyleMapper cardStyleMapper;
    private final ArtImageMapper artImageMapper;
    private final BackgroundImageMapper backgroundImageMapper;
    private final SocialButtonMapper socialButtonMapper;
    private final JsonContentReader jsonContentReader;
    private final ManagedImageUrlService managedImageUrlService;

    @Cacheable(cacheNames = "siteConfig", key = "'current'")
    public JsonNode getSiteConfig() {
        SiteConfig siteConfig = siteConfigMapper.selectById(1);
        ObjectNode storedConfig = siteConfig == null ? jsonContentReader.newObjectNode() : (ObjectNode) jsonContentReader.readObjectOrEmpty(siteConfig.getTheme());
        return toPublicSiteConfig(storedConfig);
    }

    @Cacheable(cacheNames = "cardStyles", key = "'all'")
    public JsonNode getCardStyles() {
        return toCardStylesObject(listCardStyles());
    }

    @Transactional
    @CacheEvict(cacheNames = "siteConfig", allEntries = true)
    public JsonNode saveSiteConfig(JsonNode request) {
        NormalizedSiteConfig normalizedConfig = normalizeSiteConfig(request);
        persistSiteConfig(normalizedConfig);
        return toPublicSiteConfig(normalizedConfig.config());
    }

    @Transactional
    @CacheEvict(cacheNames = "cardStyles", allEntries = true)
    public JsonNode saveCardStyles(JsonNode request) {
        List<CardStyle> cardStyles = normalizeCardStyles(request);
        replaceCardStyles(cardStyles);
        return toCardStylesObject(cardStyles);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = "siteConfig", allEntries = true),
            @CacheEvict(cacheNames = "cardStyles", allEntries = true)
    })
    public SiteSettingsVo saveSiteSettings(SiteSettingsSaveRequest request) {
        NormalizedSiteConfig normalizedConfig = normalizeSiteConfig(request.config());
        List<CardStyle> cardStyles = normalizeCardStyles(request.cardStyles());
        persistSiteConfig(normalizedConfig);
        replaceCardStyles(cardStyles);
        return new SiteSettingsVo(toPublicSiteConfig(normalizedConfig.config()), toCardStylesObject(cardStyles));
    }

    private void persistSiteConfig(NormalizedSiteConfig normalizedConfig) {
        SiteConfig siteConfig = siteConfigMapper.selectById(1);
        if (siteConfig == null) {
            siteConfig = new SiteConfig();
            siteConfig.setId(1);
            siteConfig.setTheme(jsonContentReader.write(normalizedConfig.config()));
            if (siteConfigMapper.insert(siteConfig) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "站点配置保存失败");
            }
        } else {
            siteConfig.setTheme(jsonContentReader.write(normalizedConfig.config()));
            if (siteConfigMapper.updateById(siteConfig) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "站点配置保存失败");
            }
        }
        replaceArtImages(normalizedConfig.artImages());
        replaceBackgroundImages(normalizedConfig.backgroundImages());
        replaceSocialButtons(normalizedConfig.socialButtons());
    }

    private NormalizedSiteConfig normalizeSiteConfig(JsonNode request) {
        ObjectNode config = requireObject(request, "站点配置必须是 JSON 对象").deepCopy();
        validateMeta(config);
        validateHiCard(config);
        validateTheme(config);
        validateBackgroundColors(config);
        validateHat(config);
        normalizeRootImageUrl(config, "faviconUrl");
        normalizeRootImageUrl(config, "avatarUrl");

        List<ArtImage> artImages = normalizeArtImages(config);
        List<BackgroundImage> backgroundImages = normalizeBackgroundImages(config);
        List<SocialButton> socialButtons = normalizeSocialButtons(config);
        validateCurrentImageId(config, "currentArtImageId", artImages.stream().map(ArtImage::getId).toList(), "首页图片");
        validateCurrentImageId(config, "currentBackgroundImageId", backgroundImages.stream().map(BackgroundImage::getId).toList(), "背景图片");
        return new NormalizedSiteConfig(config, artImages, backgroundImages, socialButtons);
    }

    private void validateMeta(ObjectNode config) {
        ObjectNode meta = requireObject(config.get("meta"), "站点 meta 配置不能为空");
        requiredText(meta, "title", 200, "站点标题不能为空");
        requiredText(meta, "description", 500, "站点描述不能为空");
        optionalText(meta, "username", 100, "用户名不能超过 100 个字符");
    }

    private void validateHiCard(ObjectNode config) {
        JsonNode hiCard = config.get("hiCard");
        if (hiCard == null || hiCard.isNull()) {
            return;
        }
        ObjectNode hiCardObject = requireObject(hiCard, "首页问候卡片配置必须是 JSON 对象");
        optionalText(hiCardObject, "greeting", 100, "首页问候语不能超过 100 个字符");
        optionalText(hiCardObject, "introPrefix", 50, "首页问候前缀不能超过 50 个字符");
        optionalText(hiCardObject, "introSuffix", 200, "首页问候结尾不能超过 200 个字符");
        String avatarLink = optionalText(hiCardObject, "avatarLink", 1000, "首页头像链接不能超过 1000 个字符");
        if (avatarLink != null && !avatarLink.startsWith("/") && !avatarLink.matches("(?i)^https?://.+")) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "首页头像链接必须是站内路径或 HTTP(S) URL");
        }
    }

    private void validateTheme(ObjectNode config) {
        JsonNode theme = config.get("theme");
        if (theme == null || theme.isNull()) {
            return;
        }
        ObjectNode themeObject = requireObject(theme, "主题配置必须是 JSON 对象");
        for (String key : List.of("colorBrand", "colorBrandSecondary", "colorPrimary", "colorSecondary", "colorBg", "colorBorder", "colorCard", "colorArticle")) {
            JsonNode color = themeObject.get(key);
            if (color == null || color.isNull()) {
                continue;
            }
            if (!color.isTextual() || !COLOR_PATTERN.matcher(color.asText()).matches()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "主题颜色格式不合法：" + key);
            }
        }
    }

    private void validateBackgroundColors(ObjectNode config) {
        JsonNode colors = config.get("backgroundColors");
        if (colors == null || colors.isNull()) {
            return;
        }
        if (!colors.isArray() || colors.isEmpty() || colors.size() > 8) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "背景颜色数量必须在 1 到 8 之间");
        }
        for (JsonNode color : colors) {
            if (!color.isTextual() || !COLOR_PATTERN.matcher(color.asText()).matches()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "背景颜色格式不合法");
            }
        }
    }

    private void validateHat(ObjectNode config) {
        JsonNode index = config.get("currentHatIndex");
        if (index != null && !index.isNull() && (!index.canConvertToInt() || index.asInt() < 1 || index.asInt() > 24)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "帽子索引必须在 1 到 24 之间");
        }
    }

    private void normalizeRootImageUrl(ObjectNode config, String fieldName) {
        JsonNode value = config.get(fieldName);
        if (value == null || value.isNull()) {
            return;
        }
        String url = requiredText(config, fieldName, 1000, fieldName + " 不能为空");
        config.put(fieldName, managedImageUrlService.normalizeAndValidate(url, ImageUploadScope.SITE).storedUrl());
    }

    private List<ArtImage> normalizeArtImages(ObjectNode config) {
        ArrayNode images = optionalArray(config, "artImages", MAX_IMAGE_ITEMS, "首页图片");
        ArrayNode normalizedImages = images.arrayNode();
        Set<String> ids = new HashSet<>();
        List<ArtImage> result = new ArrayList<>();
        for (int index = 0; index < images.size(); index++) {
            ObjectNode item = requireObject(images.get(index), "首页图片项必须是 JSON 对象");
            String id = resourceId(item, "首页图片 ID 不合法");
            if (!ids.add(id)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "首页图片 ID 不能重复");
            }
            String url = requiredText(item, "url", 1000, "首页图片 URL 不能为空");
            String storedUrl = managedImageUrlService.normalizeAndValidate(url, ImageUploadScope.SITE).storedUrl();
            ObjectNode normalizedItem = item.deepCopy();
            normalizedItem.put("url", storedUrl);
            normalizedImages.add(normalizedItem);

            ArtImage image = new ArtImage();
            image.setId(id);
            image.setUrl(storedUrl);
            image.setDescription(optionalText(item, "description", 10000, "首页图片说明不能超过 10000 个字符"));
            image.setSortOrder(index);
            result.add(image);
        }
        config.set("artImages", normalizedImages);
        return result;
    }

    private List<BackgroundImage> normalizeBackgroundImages(ObjectNode config) {
        ArrayNode images = optionalArray(config, "backgroundImages", MAX_IMAGE_ITEMS, "背景图片");
        ArrayNode normalizedImages = images.arrayNode();
        Set<String> ids = new HashSet<>();
        List<BackgroundImage> result = new ArrayList<>();
        for (JsonNode node : images) {
            ObjectNode item = requireObject(node, "背景图片项必须是 JSON 对象");
            String id = textOrNull(item.get("id"));
            String url = textOrNull(item.get("url"));
            if (id == null && url == null) {
                continue;
            }
            if (id == null || url == null) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "背景图片 ID 和 URL 必须同时填写");
            }
            validateResourceId(id, "背景图片 ID 不合法");
            if (!ids.add(id)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "背景图片 ID 不能重复");
            }
            if (url.length() > 1000) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "背景图片 URL 不能超过 1000 个字符");
            }
            String storedUrl = managedImageUrlService.normalizeAndValidate(url, ImageUploadScope.SITE).storedUrl();
            ObjectNode normalizedItem = item.deepCopy();
            normalizedItem.put("url", storedUrl);
            normalizedImages.add(normalizedItem);

            BackgroundImage image = new BackgroundImage();
            image.setId(id);
            image.setUrl(storedUrl);
            image.setSortOrder(result.size());
            result.add(image);
        }
        config.set("backgroundImages", normalizedImages);
        return result;
    }

    private List<SocialButton> normalizeSocialButtons(ObjectNode config) {
        ArrayNode buttons = optionalArray(config, "socialButtons", MAX_SOCIAL_BUTTONS, "社交按钮");
        ArrayNode normalizedButtons = buttons.arrayNode();
        Set<String> ids = new HashSet<>();
        List<SocialButton> result = new ArrayList<>();
        for (int index = 0; index < buttons.size(); index++) {
            ObjectNode item = requireObject(buttons.get(index), "社交按钮项必须是 JSON 对象");
            String id = resourceId(item, "社交按钮 ID 不合法");
            if (!ids.add(id)) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "社交按钮 ID 不能重复");
            }
            String type = requiredText(item, "type", 40, "社交按钮类型不能为空");
            String value = requiredText(item, "value", 1000, "社交按钮内容不能为空");
            String storedValue = managedImageUrlService.normalizeAndValidate(value, ImageUploadScope.SITE).storedUrl();
            Integer order = optionalInteger(item, "order", 1, 1000, "社交按钮顺序必须在 1 到 1000 之间");
            ObjectNode normalizedItem = item.deepCopy();
            normalizedItem.put("value", storedValue);
            normalizedItem.put("order", order == null ? index + 1 : order);
            normalizedButtons.add(normalizedItem);

            SocialButton button = new SocialButton();
            button.setId(id);
            button.setType(type);
            button.setValue(storedValue);
            button.setLabel(optionalText(item, "label", 100, "社交按钮标签不能超过 100 个字符"));
            button.setSortOrder(order == null ? index : order - 1);
            result.add(button);
        }
        config.set("socialButtons", normalizedButtons);
        return result;
    }

    private List<CardStyle> normalizeCardStyles(JsonNode request) {
        ObjectNode styles = requireObject(request, "卡片样式必须是 JSON 对象");
        if (styles.isEmpty() || styles.size() > 50) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "卡片样式数量必须在 1 到 50 之间");
        }
        List<CardStyle> result = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = styles.fields();
        int index = 0;
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!CARD_KEY_PATTERN.matcher(entry.getKey()).matches()) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "卡片 key 不合法：" + entry.getKey());
            }
            ObjectNode config = requireObject(entry.getValue(), "卡片样式必须是 JSON 对象").deepCopy();
            Integer width = optionalInteger(config, "width", 0, 4000, "卡片宽度必须在 0 到 4000 之间");
            Integer height = optionalInteger(config, "height", 0, 4000, "卡片高度必须在 0 到 4000 之间");
            Integer offsetX = optionalInteger(config, "offsetX", -5000, 5000, "卡片横向偏移必须在 -5000 到 5000 之间");
            Integer offsetY = optionalInteger(config, "offsetY", -5000, 5000, "卡片纵向偏移必须在 -5000 到 5000 之间");
            Integer order = optionalInteger(config, "order", 0, 1000, "卡片顺序必须在 0 到 1000 之间");
            boolean enabled = optionalBoolean(config, "enabled", true, "卡片启用状态必须是布尔值");
            config.put("order", order == null ? index : order);
            config.put("enabled", enabled);

            CardStyle cardStyle = new CardStyle();
            cardStyle.setCardKey(entry.getKey());
            cardStyle.setWidth(width);
            cardStyle.setHeight(height);
            cardStyle.setOffsetX(offsetX);
            cardStyle.setOffsetY(offsetY);
            cardStyle.setSortOrder(order == null ? index : order);
            cardStyle.setEnabled(enabled);
            cardStyle.setConfig(jsonContentReader.write(config));
            result.add(cardStyle);
            index++;
        }
        return result;
    }

    private void replaceArtImages(List<ArtImage> images) {
        artImageMapper.delete(Wrappers.emptyWrapper());
        for (ArtImage image : images) {
            if (artImageMapper.insert(image) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "首页图片保存失败");
            }
        }
    }

    private void replaceBackgroundImages(List<BackgroundImage> images) {
        backgroundImageMapper.delete(Wrappers.emptyWrapper());
        for (BackgroundImage image : images) {
            if (backgroundImageMapper.insert(image) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "背景图片保存失败");
            }
        }
    }

    private void replaceSocialButtons(List<SocialButton> buttons) {
        socialButtonMapper.delete(Wrappers.emptyWrapper());
        for (SocialButton button : buttons) {
            if (socialButtonMapper.insert(button) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "社交按钮保存失败");
            }
        }
    }

    private void replaceCardStyles(List<CardStyle> cardStyles) {
        cardStyleMapper.delete(Wrappers.emptyWrapper());
        for (CardStyle cardStyle : cardStyles) {
            if (cardStyleMapper.insert(cardStyle) != 1) {
                throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "卡片样式保存失败");
            }
        }
    }

    private List<CardStyle> listCardStyles() {
        return cardStyleMapper.selectList(Wrappers.<CardStyle>lambdaQuery()
                .orderByAsc(CardStyle::getSortOrder)
                .orderByAsc(CardStyle::getId));
    }

    private ObjectNode toCardStylesObject(List<CardStyle> cardStyles) {
        ObjectNode result = jsonContentReader.newObjectNode();
        cardStyles.forEach(cardStyle -> addCardStyle(result, cardStyle));
        return result;
    }

    private void addCardStyle(ObjectNode result, CardStyle cardStyle) {
        JsonNode config = jsonContentReader.read(cardStyle.getConfig());
        if (config != null && config.isObject()) {
            result.set(cardStyle.getCardKey(), config);
            return;
        }
        ObjectNode fallback = result.putObject(cardStyle.getCardKey());
        putNullableInt(fallback, "width", cardStyle.getWidth());
        putNullableInt(fallback, "height", cardStyle.getHeight());
        putNullableInt(fallback, "offsetX", cardStyle.getOffsetX());
        putNullableInt(fallback, "offsetY", cardStyle.getOffsetY());
        fallback.put("order", cardStyle.getSortOrder());
        fallback.put("enabled", Boolean.TRUE.equals(cardStyle.getEnabled()));
    }

    private ObjectNode toPublicSiteConfig(ObjectNode storedConfig) {
        ObjectNode result = storedConfig.deepCopy();
        toPublicRootImageUrl(result, "faviconUrl");
        toPublicRootImageUrl(result, "avatarUrl");
        toPublicArrayImageUrls(result, "artImages", "url");
        toPublicArrayImageUrls(result, "backgroundImages", "url");
        toPublicArrayImageUrls(result, "socialButtons", "value");
        return result;
    }

    private void toPublicRootImageUrl(ObjectNode config, String fieldName) {
        JsonNode url = config.get(fieldName);
        if (url != null && url.isTextual()) {
            config.put(fieldName, managedImageUrlService.toPublicUrl(url.asText(), ImageUploadScope.SITE));
        }
    }

    private void toPublicArrayImageUrls(ObjectNode config, String arrayName, String fieldName) {
        JsonNode array = config.get(arrayName);
        if (array == null || !array.isArray()) {
            return;
        }
        for (JsonNode item : array) {
            if (item instanceof ObjectNode objectNode && objectNode.path(fieldName).isTextual()) {
                objectNode.put(fieldName, managedImageUrlService.toPublicUrl(objectNode.path(fieldName).asText(), ImageUploadScope.SITE));
            }
        }
    }

    private ObjectNode requireObject(JsonNode node, String message) {
        if (node == null || !node.isObject()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return (ObjectNode) node;
    }

    private ArrayNode optionalArray(ObjectNode object, String fieldName, int maximumSize, String label) {
        JsonNode node = object.get(fieldName);
        if (node == null || node.isNull()) {
            return object.arrayNode();
        }
        if (!node.isArray() || node.size() > maximumSize) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "数量不能超过 " + maximumSize);
        }
        return (ArrayNode) node;
    }

    private String requiredText(ObjectNode object, String fieldName, int maxLength, String message) {
        String value = textOrNull(object.get(fieldName));
        if (value == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        if (value.length() > maxLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + "不能超过 " + maxLength + " 个字符");
        }
        return value;
    }

    private String optionalText(ObjectNode object, String fieldName, int maxLength, String message) {
        String value = textOrNull(object.get(fieldName));
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return value;
    }

    private String resourceId(ObjectNode object, String message) {
        String id = textOrNull(object.get("id"));
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        validateResourceId(id, message);
        return id;
    }

    private void validateResourceId(String id, String message) {
        if (!RESOURCE_ID_PATTERN.matcher(id).matches()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
    }

    private Integer optionalInteger(ObjectNode object, String fieldName, int min, int max, String message) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt() || value.asInt() < min || value.asInt() > max) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return value.asInt();
    }

    private boolean optionalBoolean(ObjectNode object, String fieldName, boolean defaultValue, String message) {
        JsonNode value = object.get(fieldName);
        if (value == null || value.isNull()) {
            return defaultValue;
        }
        if (!value.isBoolean()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, message);
        }
        return value.asBoolean();
    }

    private void validateCurrentImageId(ObjectNode config, String fieldName, List<String> ids, String label) {
        String currentId = textOrNull(config.get(fieldName));
        if (currentId != null && !ids.contains(currentId)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, label + "当前选择不存在");
        }
    }

    private String textOrNull(JsonNode node) {
        if (node == null || !node.isTextual()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private void putNullableInt(ObjectNode node, String fieldName, Integer value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }

    private record NormalizedSiteConfig(
            ObjectNode config,
            List<ArtImage> artImages,
            List<BackgroundImage> backgroundImages,
            List<SocialButton> socialButtons
    ) {
    }
}
