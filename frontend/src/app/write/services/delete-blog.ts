import { deleteAdminBlog } from '@/lib/admin-blog-api'

export async function deleteBlog(id: number): Promise<void> {
	if (!Number.isInteger(id) || id <= 0) {
		throw new Error('缺少有效的文章 ID，无法删除')
	}
	await deleteAdminBlog(id)
}
