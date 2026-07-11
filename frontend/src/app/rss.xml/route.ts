import { getPublicSiteUrl, getServerBlogs, getServerSiteConfig } from '@/lib/server-public-api'

export const revalidate = 300

const escapeXml = (value: string): string =>
	value.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&apos;')

const wrapCdata = (value: string): string => `<![CDATA[${value.replace(/]]>/g, ']]]]><![CDATA[>')}]]>`

export async function GET(): Promise<Response> {
	const siteUrl = getPublicSiteUrl()
	const [siteResult, blogsResult] = await Promise.allSettled([getServerSiteConfig(), getServerBlogs()])
	const title = siteResult.status === 'fulfilled' ? siteResult.value.meta.title : 'NewBlog'
	const description = siteResult.status === 'fulfilled' ? siteResult.value.meta.description : '博客更新'
	const blogs = blogsResult.status === 'fulfilled' ? blogsResult.value : []
	const failedResults = [siteResult, blogsResult].filter(
		(result): result is PromiseRejectedResult => result.status === 'rejected'
	)

	if (failedResults.length > 0) {
		console.warn('生成 RSS 时读取公开内容失败，将返回可用的降级订阅：', failedResults.map(result => result.reason))
	}

	const items = blogs
		.filter(blog => Boolean(blog.slug))
		.map(blog => {
			const link = `${siteUrl}/blog/${encodeURIComponent(blog.slug)}`
			const categories = (blog.tags || [])
				.filter(Boolean)
				.map(tag => `<category>${escapeXml(tag)}</category>`)
				.join('')
			const publishedAt = blog.date ? new Date(blog.date) : new Date()
			return `
		<item>
			<title>${escapeXml(blog.title || blog.slug)}</title>
			<link>${link}</link>
			<guid isPermaLink="true">${link}</guid>
			<description>${wrapCdata(blog.summary || '')}</description>
			<pubDate>${publishedAt.toUTCString()}</pubDate>
			${categories}
		</item>`.trim()
		})
		.join('\n')

	const rss = `<?xml version="1.0" encoding="UTF-8"?>
<rss version="2.0">
	<channel xmlns:atom="http://www.w3.org/2005/Atom">
		<title>${escapeXml(title)}</title>
		<link>${siteUrl}</link>
		<atom:link href="${siteUrl}/rss.xml" rel="self" type="application/rss+xml" />
		<description>${escapeXml(description)}</description>
		<language>zh-CN</language>
		<docs>https://www.rssboard.org/rss-specification</docs>
		<ttl>5</ttl>
		<lastBuildDate>${new Date().toUTCString()}</lastBuildDate>
		${items}
	</channel>
</rss>`

	return new Response(rss, {
		headers: {
			'Content-Type': 'application/rss+xml; charset=utf-8',
			'Cache-Control': 'public, s-maxage=300, stale-while-revalidate=600'
		}
	})
}
