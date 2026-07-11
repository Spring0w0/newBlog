import type { MetadataRoute } from 'next'

import { getPublicSiteUrl } from '@/lib/server-public-api'

export default function robots(): MetadataRoute.Robots {
	const siteUrl = getPublicSiteUrl()
	return {
		rules: {
			userAgent: '*',
			allow: '/',
			disallow: ['/write', '/image-toolbox']
		},
		sitemap: `${siteUrl}/sitemap.xml`
	}
}
