import type { MetadataRoute } from 'next'

import { getPublicSiteUrl, getServerBlogs } from '@/lib/server-public-api'

export const revalidate = 300

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
	const baseUrl = getPublicSiteUrl()
	let posts: Awaited<ReturnType<typeof getServerBlogs>> = []
	try {
		posts = await getServerBlogs()
	} catch (error) {
		console.warn('生成 sitemap 时读取公开文章失败，将仅返回静态页面：', error)
	}

	const postEntries: MetadataRoute.Sitemap = posts.map(post => ({
		url: `${baseUrl}/blog/${encodeURIComponent(post.slug)}`,
		lastModified: post.date ? new Date(post.date) : new Date(),
		changeFrequency: 'weekly',
		priority: 0.8
	}))

	return [
		{
			url: baseUrl,
			lastModified: new Date(),
			changeFrequency: 'daily',
			priority: 1
		},
		...postEntries
	]
}
