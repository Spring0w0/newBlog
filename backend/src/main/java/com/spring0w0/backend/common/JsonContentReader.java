package com.spring0w0.backend.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一解析数据库 JSON 字段，避免各资源 Service 重复处理空值和非法 JSON。
 */
@Component
@RequiredArgsConstructor
public class JsonContentReader {

    private final ObjectMapper objectMapper;

    public JsonNode read(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("数据库中的 JSON 内容不合法", exception);
        }
    }

    public JsonNode readObjectOrEmpty(String value) {
        JsonNode jsonNode = read(value);
        return jsonNode != null && jsonNode.isObject() ? jsonNode : JsonNodeFactory.instance.objectNode();
    }

    public ObjectNode newObjectNode() {
        return JsonNodeFactory.instance.objectNode();
    }

    public List<String> readStringList(String value) {
        JsonNode jsonNode = read(value);
        if (jsonNode == null || !jsonNode.isArray()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        jsonNode.forEach(item -> {
            if (item.isTextual()) {
                result.add(item.asText());
            }
        });
        return result;
    }

    public String writeStringList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("无法序列化字符串列表", exception);
        }
    }
}
