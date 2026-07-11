package com.spring0w0.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring0w0.backend.common.JsonContentReader;
import com.spring0w0.backend.mapper.CardStyleMapper;
import com.spring0w0.backend.mapper.SiteConfigMapper;
import com.spring0w0.backend.pojo.entity.CardStyle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteServiceTests {

    @Mock
    private SiteConfigMapper siteConfigMapper;

    @Mock
    private CardStyleMapper cardStyleMapper;

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
}
