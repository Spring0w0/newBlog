import { useCallback } from 'react'
import { mutate } from 'swr'
import { toast } from 'sonner'
import { pushBlog } from '../services/push-blog'
import { deleteBlog } from '../services/delete-blog'
import { useWriteStore } from '../stores/write-store'
import { useAuthStore } from '@/hooks/use-auth'

export function usePublish() {
	const { loading, setLoading, form, cover, images, mode, originalId, setMode, reset } = useWriteStore()
	const { isAuth, openLoginDialog } = useAuthStore()

	const onPublish = useCallback(async () => {
		try {
			setLoading(true)
			const blog = await pushBlog({ form, cover, images, mode, originalId })
			setMode('edit', blog.slug, blog.id)
			void mutate('/api/blogs')
			void mutate('/api/categories')
			toast.success(mode === 'edit' ? '更新成功' : '发布成功')
		} catch (error: unknown) {
			console.error(error)
			toast.error(error instanceof Error ? error.message : '操作失败')
		} finally {
			setLoading(false)
		}
	}, [cover, form, images, mode, originalId, setLoading, setMode])

	const onDelete = useCallback(async (): Promise<boolean> => {
		if (!originalId) {
			toast.error('缺少文章 ID，无法删除')
			return false
		}
		try {
			setLoading(true)
			await deleteBlog(originalId)
			await Promise.all([mutate('/api/blogs'), mutate('/api/categories')])
			reset()
			toast.success('删除成功')
			return true
		} catch (error: unknown) {
			console.error(error)
			toast.error(error instanceof Error ? error.message : '删除失败')
			return false
		} finally {
			setLoading(false)
		}
	}, [originalId, reset, setLoading])

	return {
		isAuth,
		loading,
		openLoginDialog,
		onPublish,
		onDelete
	}
}
