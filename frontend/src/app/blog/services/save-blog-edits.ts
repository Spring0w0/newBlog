import {
	deleteAdminBlogs,
	getAdminBlog,
	getAllAdminBlogSummaries,
	saveAdminCategories,
	updateAdminBlog,
	type BlogWritePayload
} from '@/lib/admin-blog-api'
import { getPublic } from '@/lib/public-api'
import type { BlogIndexItem } from '@/app/blog/types'

/**
 * 保存博客列表编辑页的删除、分类归属与分类排序。
 * 分类新增必须先落库，删除分类则必须等相关文章迁移或删除后才可执行。
 */
export async function saveBlogEdits(originalItems: BlogIndexItem[], nextItems: BlogIndexItem[], categories: string[]): Promise<void> {
	const [summaries, currentCategories] = await Promise.all([getAllAdminBlogSummaries(), getPublic<string[]>('/api/categories')])
	const idBySlug = new Map(summaries.map(item => [item.slug, item.id]))
	const nextBySlug = new Map(nextItems.map(item => [item.slug, item]))
	const originalBySlug = new Map(originalItems.map(item => [item.slug, item]))

	const missingSlugs = originalItems.map(item => item.slug).filter(slug => !idBySlug.has(slug))
	if (missingSlugs.length > 0) {
		throw new Error('文章数据已变化，请刷新页面后重试')
	}

	const desiredCategories = normalizeCategories(categories)
	const bootstrapCategories = normalizeCategories([...desiredCategories, ...currentCategories])
	await saveAdminCategories(bootstrapCategories)

	for (const [slug, nextItem] of nextBySlug) {
		const originalItem = originalBySlug.get(slug)
		if (!originalItem || (originalItem.category || '') === (nextItem.category || '')) {
			continue
		}
		const id = idBySlug.get(slug)
		if (!id) {
			throw new Error('文章数据已变化，请刷新页面后重试')
		}
		const blog = await getAdminBlog(id)
		const payload: BlogWritePayload = {
			title: blog.title,
			slug: blog.slug,
			markdown: blog.markdown,
			summary: blog.summary || undefined,
			tags: blog.tags,
			category: nextItem.category || undefined,
			cover: blog.cover || undefined,
			hidden: blog.hidden,
			publishedAt: blog.publishedAt || undefined
		}
		await updateAdminBlog(id, payload)
	}

	const removedIds = originalItems
		.filter(item => !nextBySlug.has(item.slug))
		.map(item => idBySlug.get(item.slug))
		.filter((id): id is number => id !== undefined)
	if (removedIds.length > 0) {
		await deleteAdminBlogs(removedIds)
	}

	await saveAdminCategories(desiredCategories)
}

function normalizeCategories(categories: string[]): string[] {
	return Array.from(new Set(categories.map(category => category.trim()).filter(Boolean)))
}
