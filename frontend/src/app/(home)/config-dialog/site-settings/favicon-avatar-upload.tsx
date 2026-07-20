'use client'

import { useRef } from 'react'
import { toast } from 'sonner'
import { deleteUploadedImage, uploadImage } from '@/lib/file-api'
import type { FileItem } from './types'
import type { SiteContent } from '../../stores/config-store'
import { resolveAvatarUrl } from '../../stores/config-defaults'

interface FaviconAvatarUploadProps {
	formData: SiteContent
	setFormData: React.Dispatch<React.SetStateAction<SiteContent>>
	faviconItem: FileItem | null
	setFaviconItem: React.Dispatch<React.SetStateAction<FileItem | null>>
	avatarItem: FileItem | null
	setAvatarItem: React.Dispatch<React.SetStateAction<FileItem | null>>
}

export function FaviconAvatarUpload({ formData, setFormData, faviconItem, setFaviconItem, avatarItem, setAvatarItem }: FaviconAvatarUploadProps) {
	const faviconInputRef = useRef<HTMLInputElement>(null)
	const avatarInputRef = useRef<HTMLInputElement>(null)

	const handleFaviconFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
		const file = e.target.files?.[0]
		if (!file) return

		try {
			const uploaded = await uploadImage(file, 'site')
			if (faviconItem?.type === 'url' && faviconItem.fileId) {
				await deleteUploadedImage(faviconItem.fileId)
			}
			setFaviconItem({ type: 'url', url: uploaded.url, fileId: uploaded.fileId })
			setFormData(prev => ({ ...prev, faviconUrl: uploaded.url }))
		} catch (error) {
			toast.error(error instanceof Error ? error.message : 'Favicon 上传失败')
		} finally {
			if (e.currentTarget) e.currentTarget.value = ''
		}
	}

	const handleAvatarFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
		const file = e.target.files?.[0]
		if (!file) return

		try {
			const uploaded = await uploadImage(file, 'site')
			if (avatarItem?.type === 'url' && avatarItem.fileId) {
				await deleteUploadedImage(avatarItem.fileId)
			}
			setAvatarItem({ type: 'url', url: uploaded.url, fileId: uploaded.fileId })
			setFormData(prev => ({ ...prev, avatarUrl: uploaded.url }))
		} catch (error) {
			toast.error(error instanceof Error ? error.message : '头像上传失败')
		} finally {
			if (e.currentTarget) e.currentTarget.value = ''
		}
	}

	return (
		<div className='grid grid-cols-2 gap-4'>
			<div>
				<label className='mb-2 block text-sm font-medium'>Favicon</label>
				<input ref={faviconInputRef} type='file' accept='image/*' className='hidden' onChange={handleFaviconFileSelect} />
				<div className='group relative h-20 w-20 cursor-pointer overflow-hidden rounded-lg border bg-white/60'>
					{faviconItem || formData.faviconUrl ? (
						<img
							src={faviconItem ? (faviconItem.type === 'file' ? faviconItem.previewUrl : faviconItem.url) : formData.faviconUrl}
							alt='favicon preview'
							className='h-full w-full object-cover'
						/>
					) : (
						<img src='/favicon.png' alt='current favicon' className='h-full w-full object-cover' />
					)}
					<div className='pointer-events-none absolute inset-0 flex items-center justify-center rounded-lg bg-black/40 opacity-0 transition-opacity group-hover:opacity-100'>
						<span className='text-xs text-white'>{faviconItem ? '更换' : '上传'}</span>
					</div>

					<div className='absolute inset-0' onClick={() => faviconInputRef.current?.click()} />
				</div>
			</div>

			<div>
				<label className='mb-2 block text-sm font-medium'>Avatar</label>
				<input ref={avatarInputRef} type='file' accept='image/*' className='hidden' onChange={handleAvatarFileSelect} />
				<div className='group relative h-20 w-20 cursor-pointer overflow-hidden rounded-full border bg-white/60'>
					{avatarItem || formData.avatarUrl ? (
						<img
							src={avatarItem ? (avatarItem.type === 'file' ? avatarItem.previewUrl : avatarItem.url) : resolveAvatarUrl(formData.avatarUrl)}
							alt='avatar preview'
							className='h-full w-full object-cover'
						/>
					) : (
						<img src={resolveAvatarUrl()} alt='current avatar' className='h-full w-full object-cover' />
					)}
					<div className='pointer-events-none absolute inset-0 flex items-center justify-center rounded-full bg-black/40 opacity-0 transition-opacity group-hover:opacity-100'>
						<span className='text-xs text-white'>{avatarItem ? '更换' : '上传'}</span>
					</div>
					<div className='absolute inset-0' onClick={() => avatarInputRef.current?.click()} />
				</div>
			</div>
		</div>
	)
}
