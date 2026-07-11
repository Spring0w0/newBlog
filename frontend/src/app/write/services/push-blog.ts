import { createAdminBlog, updateAdminBlog, type AdminBlog, type BlogWritePayload } from '@/lib/admin-blog-api'
import type { ImageItem } from '../types'
import { formatDateTimeLocal } from '../stores/write-store'

export type PushBlogParams = {
	form: {
		slug: string
		title: string
		md: string
		tags: string[]
		date?: string
		summary?: string
		hidden?: boolean
		category?: string
	}
	cover?: ImageItem | null
	images?: ImageItem[]
	mode?: 'create' | 'edit'
	originalId?: number | null
}

export async function pushBlog(params: PushBlogParams): Promise<AdminBlog> {
	const { form, cover, images, mode = 'create', originalId } = params
	if (!form.slug.trim()) {
		throw new Error('需要填写 slug')
	}
	if (mode === 'edit' && !originalId) {
		throw new Error('缺少文章 ID，无法更新')
	}
	if (cover?.type === 'file' || images?.some(image => image.type === 'file') || form.md.includes('local-image:')) {
		throw new Error('图片尚未上传完成，请等待上传完成后再保存')
	}

	const payload: BlogWritePayload = {
		title: form.title.trim(),
		slug: form.slug.trim(),
		markdown: form.md,
		summary: form.summary?.trim() || undefined,
		tags: Array.from(new Set(form.tags.map(tag => tag.trim()).filter(Boolean))),
		category: form.category?.trim() || undefined,
		cover: cover?.type === 'url' ? cover.url : undefined,
		hidden: Boolean(form.hidden),
		publishedAt: form.date || formatDateTimeLocal()
	}

	return mode === 'edit' ? updateAdminBlog(originalId!, payload) : createAdminBlog(payload)
}
