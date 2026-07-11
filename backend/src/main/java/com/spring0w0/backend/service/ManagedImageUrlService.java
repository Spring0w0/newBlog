package com.spring0w0.backend.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.common.ResultCode;
import com.spring0w0.backend.config.UploadProperties;
import com.spring0w0.backend.exception.BusinessException;
import com.spring0w0.backend.mapper.FileAssetMapper;
import com.spring0w0.backend.pojo.entity.FileAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * 规范化业务中的受管图片 URL，并校验其 scope 与文件元数据引用。
 */
@Service
@RequiredArgsConstructor
public class ManagedImageUrlService {

    private final FileAssetMapper fileAssetMapper;
    private final UploadProperties uploadProperties;

    public ManagedImageReference normalizeAndValidate(String url, ImageUploadScope expectedScope) {
        String normalizedUrl = trimToNull(url);
        String managedPath = getManagedPath(normalizedUrl, expectedScope);
        if (managedPath == null) {
            return new ManagedImageReference(normalizedUrl, null);
        }

        String relativePath = managedPath.substring(uploadProperties.normalizedPublicPath().length() + 1);
        FileAsset asset = fileAssetMapper.selectOne(Wrappers.<FileAsset>lambdaQuery()
                .eq(FileAsset::getRelativePath, relativePath));
        if (asset == null || !expectedScope.value().equals(asset.getScope())) {
            throw new BusinessException(ResultCode.UNPROCESSABLE_ENTITY, "受管图片不存在或不属于指定业务范围");
        }
        return new ManagedImageReference(managedPath, asset.getId());
    }

    public String toPublicUrl(String url, ImageUploadScope expectedScope) {
        String managedPath = getManagedPath(url, expectedScope);
        return managedPath == null ? url : uploadProperties.buildPublicUrl(managedPath);
    }

    private String getManagedPath(String url, ImageUploadScope expectedScope) {
        String normalizedUrl = trimToNull(url);
        if (normalizedUrl == null) {
            return null;
        }
        try {
            String path = new URI(normalizedUrl).getPath();
            if (path == null || path.contains("..")) {
                return null;
            }
            String publicPrefix = uploadProperties.normalizedPublicPath() + "/";
            for (ImageUploadScope scope : ImageUploadScope.values()) {
                if (!path.startsWith(publicPrefix + scope.value() + "/")) {
                    continue;
                }
                if (scope != expectedScope) {
                    throw new BusinessException(ResultCode.UNPROCESSABLE_ENTITY, "受管图片不属于指定业务范围");
                }
                return path;
            }
            return null;
        } catch (URISyntaxException exception) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record ManagedImageReference(String storedUrl, Long fileAssetId) {
    }
}
