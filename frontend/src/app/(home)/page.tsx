'use client'

import HiCard from '@/app/(home)/hi-card'
import ArtCard from '@/app/(home)/art-card'
import ClockCard from '@/app/(home)/clock-card'
import CalendarCard from '@/app/(home)/calendar-card'
import SocialButtons from '@/app/(home)/social-buttons'
import ShareCard from '@/app/(home)/share-card'
import AritcleCard from '@/app/(home)/aritcle-card'
import WriteButtons from '@/app/(home)/write-buttons'
import LikePosition from './like-position'
import HatCard from './hat-card'
import BeianCard from './beian-card'
import { useSize } from '@/hooks/use-size'
import { motion } from 'motion/react'
import { useLayoutEditStore } from './stores/layout-edit-store'
import { useConfigStore } from './stores/config-store'
import { toast } from 'sonner'
import ConfigDialog from './config-dialog/index'
import { useCallback, useEffect } from 'react'
import SnowfallBackground from '@/layout/backgrounds/snowfall'
import { useContentEditStore } from './stores/content-edit-store'
import { useAuthStore } from '@/hooks/use-auth'

export default function Home() {
	const { maxSM } = useSize()
	const { cardStyles, configDialogOpen, setConfigDialogOpen, siteContent } = useConfigStore()
	const editing = useLayoutEditStore(state => state.editing)
	const saveLayoutEditing = useLayoutEditStore(state => state.saveEditing)
	const cancelLayoutEditing = useLayoutEditStore(state => state.cancelEditing)
	const contentEditing = useContentEditStore(state => state.editing)
	const contentSaving = useContentEditStore(state => state.isSaving)
	const startContentEditing = useContentEditStore(state => state.startEditing)
	const saveContentEditing = useContentEditStore(state => state.saveEditing)
	const cancelContentEditing = useContentEditStore(state => state.cancelEditing)
	const { isAuth, openLoginDialog } = useAuthStore()

	const handleSave = () => {
		saveLayoutEditing()
		toast.success('首页布局偏移已保存（尚未提交到远程配置）')
	}

	const handleCancel = () => {
		cancelLayoutEditing()
		toast.info('已取消此次拖拽布局修改')
	}

	const handleStartContentEditing = useCallback(() => {
		if (contentEditing) return
		const start = () => {
			startContentEditing()
			toast.info('正在所见即所得编辑首页内容')
		}
		if (isAuth) {
			start()
		} else {
			openLoginDialog(start)
		}
	}, [contentEditing, isAuth, openLoginDialog, startContentEditing])

	const handleSaveContentEditing = async () => {
		try {
			await saveContentEditing()
			toast.success('首页内容已保存')
		} catch (error: any) {
			toast.error(`保存失败: ${error?.message || '未知错误'}`)
		}
	}

	const handleCancelContentEditing = async () => {
		try {
			await cancelContentEditing()
			toast.info('已取消首页内容编辑')
		} catch (error: any) {
			toast.error(`取消编辑时清理图片失败: ${error?.message || '未知错误'}`)
		}
	}

	useEffect(() => {
		const handleKeyDown = (e: KeyboardEvent) => {
			if ((e.ctrlKey || e.metaKey) && (e.key === 'l' || e.key === ',')) {
				e.preventDefault()
				setConfigDialogOpen(true)
			} else if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'e') {
				e.preventDefault()
				handleStartContentEditing()
			}
		}

		window.addEventListener('keydown', handleKeyDown)
		return () => {
			window.removeEventListener('keydown', handleKeyDown)
		}
	}, [setConfigDialogOpen, handleStartContentEditing])

	return (
		<>
			{siteContent.enableChristmas && <SnowfallBackground zIndex={0} count={!maxSM ? 125 : 20} />}

			{editing && (
				<div className='pointer-events-none fixed inset-x-0 top-0 z-50 flex justify-center pt-6'>
					<div className='pointer-events-auto flex items-center gap-3 rounded-2xl bg-white/80 px-4 py-2 shadow-lg backdrop-blur'>
						<span className='text-xs text-gray-600'>正在编辑首页布局，拖拽卡片调整位置</span>
						<div className='flex gap-2'>
							<motion.button
								type='button'
								whileHover={{ scale: 1.05 }}
								whileTap={{ scale: 0.95 }}
								onClick={handleCancel}
								className='rounded-xl border bg-white px-3 py-1 text-xs font-medium text-gray-700'>
								取消
							</motion.button>
							<motion.button type='button' whileHover={{ scale: 1.05 }} whileTap={{ scale: 0.95 }} onClick={handleSave} className='brand-btn px-3 py-1 text-xs'>
								保存偏移
							</motion.button>
						</div>
					</div>
				</div>
			)}

			{contentEditing && (
				<div className='pointer-events-none fixed inset-x-0 top-0 z-50 flex justify-center pt-6'>
					<div className='pointer-events-auto flex items-center gap-3 rounded-2xl bg-white/85 px-4 py-2 shadow-lg backdrop-blur'>
						<span className='text-xs text-gray-600'>正在编辑首页内容，图片和文字会实时显示在卡片上</span>
						<div className='flex gap-2'>
							<motion.button
								type='button'
								whileHover={{ scale: 1.05 }}
								whileTap={{ scale: 0.95 }}
								onClick={() => void handleCancelContentEditing()}
								disabled={contentSaving}
								className='rounded-xl border bg-white px-3 py-1 text-xs font-medium text-gray-700 disabled:cursor-not-allowed disabled:opacity-60'>
								取消
							</motion.button>
							<motion.button
								type='button'
								whileHover={{ scale: 1.05 }}
								whileTap={{ scale: 0.95 }}
								onClick={() => void handleSaveContentEditing()}
								disabled={contentSaving}
								className='brand-btn px-3 py-1 text-xs disabled:cursor-not-allowed disabled:opacity-70'>
								{contentSaving ? '保存中...' : '保存内容'}
							</motion.button>
						</div>
					</div>
				</div>
			)}

			<div className='max-sm:flex max-sm:flex-col max-sm:items-center max-sm:gap-6 max-sm:pt-28 max-sm:pb-20'>
				{cardStyles.artCard?.enabled !== false && <ArtCard />}
				{cardStyles.hiCard?.enabled !== false && <HiCard />}
				{!maxSM && cardStyles.clockCard?.enabled !== false && <ClockCard />}
				{!maxSM && cardStyles.calendarCard?.enabled !== false && <CalendarCard />}
				{cardStyles.socialButtons?.enabled !== false && <SocialButtons />}
				{!maxSM && cardStyles.shareCard?.enabled !== false && <ShareCard />}
				{cardStyles.articleCard?.enabled !== false && <AritcleCard />}
				{!maxSM && cardStyles.writeButtons?.enabled !== false && <WriteButtons />}
				{cardStyles.likePosition?.enabled !== false && <LikePosition />}
				{cardStyles.hatCard?.enabled !== false && <HatCard />}
				{cardStyles.beianCard?.enabled !== false && <BeianCard />}
			</div>

			{siteContent.enableChristmas && <SnowfallBackground zIndex={2} count={!maxSM ? 125 : 20} />}
			<ConfigDialog open={configDialogOpen} onClose={() => setConfigDialogOpen(false)} />
		</>
	)
}
