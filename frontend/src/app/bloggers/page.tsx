'use client'

import { useState, useEffect } from 'react'
import { motion } from 'motion/react'
import { mutate } from 'swr'
import { toast } from 'sonner'
import GridView, { type Blogger } from './grid-view'
import CreateDialog from './components/create-dialog'
import { pushBloggers } from './services/push-bloggers'
import { useAuthStore } from '@/hooks/use-auth'
import { useConfigStore } from '@/app/(home)/stores/config-store'
import type { AvatarItem } from './components/avatar-upload-dialog'
import { usePublicResource } from '@/hooks/use-public-resource'
import { deleteUploadedImage } from '@/lib/file-api'
import { getAdminBloggers } from '@/lib/admin-content-api'

export default function Page() {
	const [bloggers, setBloggers] = useState<Blogger[]>([])
	const [originalBloggers, setOriginalBloggers] = useState<Blogger[]>([])
	const [isEditMode, setIsEditMode] = useState(false)
	const [isSaving, setIsSaving] = useState(false)
	const [editingBlogger, setEditingBlogger] = useState<Blogger | null>(null)
	const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
	const [avatarItems, setAvatarItems] = useState<Map<string, AvatarItem>>(new Map())

	const { isAuth, openLoginDialog } = useAuthStore()
	const { siteContent } = useConfigStore()
	const { data: publicBloggers } = usePublicResource<Blogger[]>('/api/bloggers')
	const hideEditButton = siteContent.hideEditButton ?? false

	useEffect(() => {
		if (!publicBloggers || isEditMode) return
		setBloggers(publicBloggers)
		setOriginalBloggers(publicBloggers)
	}, [publicBloggers, isEditMode])

	const handleUpdate = (updatedBlogger: Blogger, oldBlogger: Blogger, avatarItem?: AvatarItem) => {
		setBloggers(prev => prev.map(b => (isSameBlogger(b, oldBlogger) ? updatedBlogger : b)))
		if (avatarItem) {
			setAvatarItems(prev => {
				const newMap = new Map(prev)
				newMap.set(updatedBlogger.url, avatarItem)
				return newMap
			})
		}
	}

	const handleAdd = () => {
		setEditingBlogger(null)
		setIsCreateDialogOpen(true)
	}

	const handleSaveBlogger = (updatedBlogger: Blogger, avatarItem?: AvatarItem) => {
		if (editingBlogger) {
			const updated = bloggers.map(b => (isSameBlogger(b, editingBlogger) ? updatedBlogger : b))
			setBloggers(updated)
		} else {
			setBloggers([...bloggers, updatedBlogger])
		}
		if (avatarItem) {
			setAvatarItems(prev => new Map(prev).set(updatedBlogger.url, avatarItem))
		}
	}

	const handleDelete = (blogger: Blogger) => {
		if (confirm(`确定要删除 ${blogger.name} 吗？`)) {
			const pendingAvatar = avatarItems.get(blogger.url)
			if (pendingAvatar?.type === 'url' && pendingAvatar.fileId) {
				void deleteUploadedImage(pendingAvatar.fileId).catch(error => console.error('清理未保存头像失败:', error))
			}
			setBloggers(bloggers.filter(b => !isSameBlogger(b, blogger)))
			setAvatarItems(prev => {
				const next = new Map(prev)
				next.delete(blogger.url)
				return next
			})
		}
	}

	const handleSaveClick = () => {
		if (!isAuth) {
			openLoginDialog()
		} else {
			void handleSave()
		}
	}

	const enterEditMode = async () => {
		if (!isAuth) {
			openLoginDialog(() => enterEditMode())
			return
		}
		try {
			const managedBloggers = await getAdminBloggers()
			setBloggers(managedBloggers)
			setOriginalBloggers(managedBloggers)
			setIsEditMode(true)
		} catch (error: any) {
			console.error('Failed to load admin bloggers:', error)
			toast.error(error?.message || '加载管理数据失败')
		}
	}

	const handleSave = async () => {
		setIsSaving(true)

		try {
			const savedBloggers = await pushBloggers({ bloggers })

			setBloggers(savedBloggers)
			setOriginalBloggers(savedBloggers)
			setAvatarItems(new Map())
			await mutate('/api/bloggers')
			setIsEditMode(false)
			toast.success('保存成功！')
		} catch (error: any) {
			console.error('Failed to save:', error)
			toast.error(`保存失败: ${error?.message || '未知错误'}`)
		} finally {
			setIsSaving(false)
		}
	}

	const handleCancel = async () => {
		const pendingFileIds = Array.from(avatarItems.values())
			.filter((item): item is Extract<AvatarItem, { type: 'url'; fileId?: number }> => item.type === 'url' && typeof item.fileId === 'number')
			.map(item => item.fileId as number)
		await Promise.allSettled(pendingFileIds.map(fileId => deleteUploadedImage(fileId)))
		setBloggers(originalBloggers)
		setAvatarItems(new Map())
		setIsEditMode(false)
	}

	const buttonText = isAuth ? '保存' : '登录'

	useEffect(() => {
		const handleKeyDown = (e: KeyboardEvent) => {
			if (!isEditMode && (e.ctrlKey || e.metaKey) && e.key === ',') {
				e.preventDefault()
				void enterEditMode()
			}
		}

		window.addEventListener('keydown', handleKeyDown)
		return () => {
			window.removeEventListener('keydown', handleKeyDown)
		}
	}, [isEditMode, isAuth])

	return (
		<>
			<GridView bloggers={bloggers} isEditMode={isEditMode} onUpdate={handleUpdate} onDelete={handleDelete} />

			<motion.div initial={{ opacity: 0, scale: 0.6 }} animate={{ opacity: 1, scale: 1 }} className='absolute top-4 right-6 flex gap-3 max-sm:hidden'>
				{isEditMode ? (
					<>
						<motion.button
							whileHover={{ scale: 1.05 }}
							whileTap={{ scale: 0.95 }}
							onClick={handleCancel}
							disabled={isSaving}
							className='rounded-xl border bg-white/60 px-6 py-2 text-sm'>
							取消
						</motion.button>
						<motion.button
							whileHover={{ scale: 1.05 }}
							whileTap={{ scale: 0.95 }}
							onClick={handleAdd}
							className='rounded-xl border bg-white/60 px-6 py-2 text-sm'>
							添加
						</motion.button>
						<motion.button whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }} onClick={handleSaveClick} disabled={isSaving} className='brand-btn px-6'>
							{isSaving ? '保存中...' : buttonText}
						</motion.button>
					</>
				) : (
					!hideEditButton && (
						<motion.button
							whileHover={{ scale: 1.05 }}
							whileTap={{ scale: 0.95 }}
							onClick={() => void enterEditMode()}
							className='bg-card rounded-xl border px-6 py-2 text-sm backdrop-blur-sm transition-colors hover:bg-white/80'>
							编辑
						</motion.button>
					)
				)}
			</motion.div>

			{isCreateDialogOpen && <CreateDialog blogger={editingBlogger} onClose={() => setIsCreateDialogOpen(false)} onSave={handleSaveBlogger} />}
		</>
	)
}

function isSameBlogger(left: Blogger, right: Blogger): boolean {
	return left.id !== undefined && right.id !== undefined ? left.id === right.id : left.url === right.url
}
