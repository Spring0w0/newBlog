import { del, get, post, put, request } from './api-client'

export type AdminBlogSummary = {
	id: number
	slug: string
	title: string
	tags: string[]
	publishedAt: string | null
	summary: string | null
	cover: string | null
	hidden: boolean
	category: string | null
}

export type AdminBlog = AdminBlogSummary & {
	markdown: string
	imageUrls: string[]
}

export type AdminBlogPage = {
	items: AdminBlogSummary[]
	page: number
	pageSize: number
	total: number
	totalPages: number
}

export type BlogWritePayload = {
	title: string
	slug: string
	markdown: string
	summary?: string
	tags: string[]
	category?: string
	cover?: string
	hidden?: boolean
	publishedAt?: string
}

export async function getAdminBlogs(params: { page?: number; pageSize?: number; slug?: string } = {}): Promise<AdminBlogPage> {
	return get<AdminBlogPage>('/api/admin/blogs', { query: params })
}

export function getAdminBlog(id: number): Promise<AdminBlog> {
	return get<AdminBlog>(`/api/admin/blogs/${id}`)
}

export async function getAdminBlogBySlug(slug: string): Promise<AdminBlog> {
	const page = await getAdminBlogs({ page: 1, pageSize: 1, slug })
	const summary = page.items[0]
	if (!summary) {
		throw new Error('文章不存在或已被删除')
	}
	return getAdminBlog(summary.id)
}

export async function getAllAdminBlogSummaries(): Promise<AdminBlogSummary[]> {
	const pageSize = 100
	let currentPage = 1
	const items: AdminBlogSummary[] = []

	while (true) {
		const page = await getAdminBlogs({ page: currentPage, pageSize })
		items.push(...page.items)
		if (currentPage >= page.totalPages || page.items.length === 0) {
			return items
		}
		currentPage += 1
	}
}

export function createAdminBlog(payload: BlogWritePayload): Promise<AdminBlog> {
	return post<AdminBlog>('/api/admin/blogs', payload)
}

export function updateAdminBlog(id: number, payload: BlogWritePayload): Promise<AdminBlog> {
	return put<AdminBlog>(`/api/admin/blogs/${id}`, payload)
}

export function deleteAdminBlog(id: number): Promise<void> {
	return del<void>(`/api/admin/blogs/${id}`)
}

export function deleteAdminBlogs(ids: number[]): Promise<void> {
	return request<void>('/api/admin/blogs/batch', { method: 'DELETE', body: { ids } })
}

export function saveAdminCategories(categories: string[]): Promise<string[]> {
	return put<string[]>('/api/admin/categories', { categories })
}
