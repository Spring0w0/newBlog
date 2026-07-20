package com.spring0w0.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.common.ImageUploadScope;
import com.spring0w0.backend.mapper.CardStyleMapper;
import com.spring0w0.backend.mapper.ArtImageMapper;
import com.spring0w0.backend.mapper.BackgroundImageMapper;
import com.spring0w0.backend.mapper.SiteConfigMapper;
import com.spring0w0.backend.mapper.SocialButtonMapper;
import com.spring0w0.backend.pojo.entity.CardStyle;
import com.spring0w0.backend.pojo.entity.SiteConfig;
import com.spring0w0.backend.pojo.entity.ArtImage;
import com.spring0w0.backend.pojo.entity.BackgroundImage;
import com.spring0w0.backend.pojo.entity.SocialButton;
import com.spring0w0.backend.pojo.dto.SiteSettingsSaveRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTests {

    @Mock
    private SiteConfigMapper siteConfigMapper;

    @Mock
    private CardStyleMapper cardStyleMapper;

    @Mock
    private ArtImageMapper artImageMapper;

    @Mock
    private BackgroundImageMapper backgroundImageMapper;

    @Mock
    private SocialButtonMapper socialButtonMapper;

    @Mock
    private ManagedImageUrlService managedImageUrlService;

    @Spy
    private JsonContentReader jsonContentReader = new JsonContentReader(new ObjectMapper());

    @InjectMocks
    private SiteService siteService;

    @Test
    void cardStyleFallsBackToLegacyColumnsWhenConfigIsAbsent() {
        CardStyle cardStyle = new CardStyle();
        cardStyle.setCardKey("blog");
        cardStyle.setWidth(2);
        cardStyle.setHeight(3);
        cardStyle.setOffsetX(-1);
        cardStyle.setOffsetY(null);
        cardStyle.setSortOrder(4);
        cardStyle.setEnabled(true);
        when(cardStyleMapper.selectList(any())).thenReturn(List.of(cardStyle));

        var styles = siteService.getCardStyles();

        assertThat(styles.path("blog").path("width").asInt()).isEqualTo(2);
        assertThat(styles.path("blog").path("offsetY").isNull()).isTrue();
        assertThat(styles.path("blog").path("enabled").asBoolean()).isTrue();
    }

    @Test
    void savesSiteSettingsInOneOperationAndNormalizesManagedImageUrls() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode config = objectMapper.readTree("""
                {
                  "meta":{"title":"测试站点","description":"测试描述"},
                  "faviconUrl":"http://localhost:8080/images/site/favicon.png",
                  "hiCard":{"greeting":"晚上好","introPrefix":"我是","introSuffix":"欢迎来玩","avatarLink":"/live2d"},
                  "backgroundColors":["#ffffff"],
                  "artImages":[{"id":"hero","url":"https://example.com/hero.png"}],
                  "backgroundImages":[{"id":"background","url":"https://example.com/background.png"}],
                  "socialButtons":[{"id":"github","type":"github","value":"https://github.com/example","order":1}]
                }
                """);
        JsonNode cardStyles = objectMapper.readTree("""
                {"hiCard":{"width":360,"height":288,"order":1,"enabled":true}}
                """);
        SiteConfig existing = new SiteConfig();
        existing.setId(1);
        when(siteConfigMapper.selectById(1)).thenReturn(existing);
        when(siteConfigMapper.updateById(org.mockito.ArgumentMatchers.<SiteConfig>any())).thenReturn(1);
        when(artImageMapper.insert(org.mockito.ArgumentMatchers.<ArtImage>any())).thenReturn(1);
        when(backgroundImageMapper.insert(org.mockito.ArgumentMatchers.<BackgroundImage>any())).thenReturn(1);
        when(socialButtonMapper.insert(org.mockito.ArgumentMatchers.<SocialButton>any())).thenReturn(1);
        when(cardStyleMapper.insert(org.mockito.ArgumentMatchers.<CardStyle>any())).thenReturn(1);
        when(managedImageUrlService.normalizeAndValidate(anyString(), eq(ImageUploadScope.SITE)))
                .thenAnswer(invocation -> new ManagedImageUrlService.ManagedImageReference(invocation.getArgument(0), null));
        when(managedImageUrlService.normalizeAndValidate("http://localhost:8080/images/site/favicon.png", ImageUploadScope.SITE))
                .thenReturn(new ManagedImageUrlService.ManagedImageReference("/images/site/favicon.png", 8L));
        when(managedImageUrlService.toPublicUrl(anyString(), eq(ImageUploadScope.SITE)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var saved = siteService.saveSiteSettings(new SiteSettingsSaveRequest(config, cardStyles));

        assertThat(saved.config().path("faviconUrl").asText()).isEqualTo("/images/site/favicon.png");
        assertThat(saved.config().path("hiCard").path("introPrefix").asText()).isEqualTo("我是");
        assertThat(saved.cardStyles().path("hiCard").path("width").asInt()).isEqualTo(360);
        verify(siteConfigMapper).updateById(argThat((SiteConfig site) -> site.getTheme().contains("/images/site/favicon.png")));
        verify(artImageMapper).insert(argThat((ArtImage image) -> image.getId().equals("hero")));
        verify(backgroundImageMapper).insert(argThat((BackgroundImage image) -> image.getId().equals("background")));
        verify(socialButtonMapper).insert(argThat((SocialButton button) -> button.getId().equals("github")));
    }
}
