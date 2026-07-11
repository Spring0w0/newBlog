import 'server-only'

import type { BlogConfig, BlogIndexItem } from '@/app/blog/types'
import type { SiteContent } from '@/app/(home)/stores/config-defaults'

type ApiEnvelope<T> = {
	code: number
	message: string
	data: T
}

export type PublicBlogDetail = {
	slug: string
	markdown: string
	config: BlogConfig
}

const PUBLIC_REVALIDATE_SECONDS = 300

export async function getServerSiteConfig(): Promise<SiteContent> {
	return fetchPublic<SiteContent>('/api/site/config', ['site-config'])
}

export async function getServerBlogs(): Promise<BlogIndexItem[]> {
	return fetchPublic<BlogIndexItem[]>('/api/blogs', ['blogs'])
}

export async function getServerBlog(slug: string): Promise<PublicBlogDetail> {
	return fetchPublic<PublicBlogDetail>(`/api/blogs/${encodeURIComponent(slug)}`, ['blogs', `blog-${slug}`])
}

export function getPublicSiteUrl(): string {
	const configured = process.env.SITE_URL || process.env.NEXT_PUBLIC_SITE_URL
	if (configured) return configured.replace(/\/+$/, '')
	if (process.env.VERCEL_URL) return `https://${process.env.VERCEL_URL}`
	return 'http://localhost:2025'
}

async function fetchPublic<T>(path: string, tags: string[]): Promise<T> {
	const response = await fetch(`${getBackendBaseUrl()}${path}`, {
		headers: { Accept: 'application/json' },
		next: {
			revalidate: PUBLIC_REVALIDATE_SECONDS,
			tags
		}
	})
	if (!response.ok) {
		throw new Error(`公开 API 请求失败：${path}，HTTP ${response.status}`)
	}

	const payload = (await response.json()) as ApiEnvelope<T>
	if (payload.code !== 200) {
		throw new Error(payload.message || `公开 API 请求失败：${path}`)
	}
	return payload.data
}

function getBackendBaseUrl(): string {
	const configured = process.env.INTERNAL_API_BASE_URL || process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080'
	return configured.replace(/\/+$/, '')
}
