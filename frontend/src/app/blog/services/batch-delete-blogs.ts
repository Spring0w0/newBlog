import { deleteAdminBlogs } from '@/lib/admin-blog-api'

export async function batchDeleteBlogs(ids: number[]): Promise<void> {
	const uniqueIds = Array.from(new Set(ids.filter(id => Number.isInteger(id) && id > 0)))
	if (uniqueIds.length === 0) {
		throw new Error('至少需要选择一篇文章')
	}
	await deleteAdminBlogs(uniqueIds)
}
