import { saveAdminSiteSettings, type SiteSettings } from '@/lib/admin-content-api'
import type { CardStyles, SiteContent } from '../stores/config-store'

/** 将配置弹窗的站点设置和卡片样式作为一个原子操作保存到后端。 */
export function pushSiteContent(siteContent: SiteContent, cardStyles: CardStyles): Promise<SiteSettings<SiteContent, CardStyles>> {
	return saveAdminSiteSettings(siteContent, cardStyles)
}
