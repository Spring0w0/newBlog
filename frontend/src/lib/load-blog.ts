import type { BlogConfig } from '@/app/blog/types'
import { getPublic } from './public-api'

export type { BlogConfig } from '@/app/blog/types'

export type LoadedBlog = {
	slug: string
	config: BlogConfig
	markdown: string
	cover?: string
}

export async function loadBlog(slug: string): Promise<LoadedBlog> {
	if (!slug) {
		throw new Error('Slug is required')
	}

	const blog = await getPublic<{ slug: string; markdown: string; config: BlogConfig }>(`/api/blogs/${encodeURIComponent(slug)}`)

	return {
		slug: blog.slug,
		config: blog.config,
		markdown: blog.markdown,
		cover: blog.config.cover
	}
}
