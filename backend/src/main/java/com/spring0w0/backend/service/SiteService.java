package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.CardStyleMapper;
import com.spring0w0.backend.mapper.SiteConfigMapper;
import com.spring0w0.backend.pojo.entity.CardStyle;
import com.spring0w0.backend.pojo.entity.SiteConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 站点和首页配置查询服务。
 */
@Service
@RequiredArgsConstructor
public class SiteService {

    private final SiteConfigMapper siteConfigMapper;
    private final CardStyleMapper cardStyleMapper;
    private final JsonContentReader jsonContentReader;

    public JsonNode getSiteConfig() {
        SiteConfig siteConfig = siteConfigMapper.selectById(1);
        return siteConfig == null ? jsonContentReader.newObjectNode() : jsonContentReader.readObjectOrEmpty(siteConfig.getTheme());
    }

    public JsonNode getCardStyles() {
        ObjectNode result = jsonContentReader.newObjectNode();
        cardStyleMapper.selectList(Wrappers.<CardStyle>lambdaQuery()
                        .orderByAsc(CardStyle::getSortOrder)
                        .orderByAsc(CardStyle::getId))
                .forEach(cardStyle -> addCardStyle(result, cardStyle));
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

    private void putNullableInt(ObjectNode node, String fieldName, Integer value) {
        if (value == null) {
            node.putNull(fieldName);
        } else {
            node.put(fieldName, value);
        }
    }
}
