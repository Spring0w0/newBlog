import type { Metadata } from 'next'

import BlogPageClient from './blog-page-client'
import { getPublicSiteUrl, getServerBlog, getServerSiteConfig } from '@/lib/server-public-api'

type PageProps = {
	params: Promise<{ id: string }>
}

export const revalidate = 300

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
	const { id: slug } = await params
	try {
		const [blog, siteConfig] = await Promise.all([getServerBlog(slug), getServerSiteConfig()])
		const title = blog.config.title || slug
		const description = blog.config.summary || siteConfig.meta.description
		const cover = blog.config.cover
		return {
			title,
			description,
			alternates: { canonical: `/blog/${encodeURIComponent(slug)}` },
			openGraph: {
				type: 'article',
				title,
				description,
				url: `${getPublicSiteUrl()}/blog/${encodeURIComponent(slug)}`,
				publishedTime: blog.config.date,
				tags: blog.config.tags,
				images: cover ? [{ url: cover }] : undefined
			},
			twitter: {
				card: cover ? 'summary_large_image' : 'summary',
				title,
				description,
				images: cover ? [cover] : undefined
			}
		}
	} catch (error) {
		console.warn(`读取文章 metadata 失败，slug=${slug}：`, error)
		return { title: slug }
	}
}

export default async function Page({ params }: PageProps) {
	const { id: slug } = await params
	return <BlogPageClient slug={slug} />
}
