import Card from '@/components/card'
import { useCenterStore } from '@/hooks/use-center'
import { useConfigStore } from './stores/config-store'
import { CARD_SPACING } from '@/consts'
import { useRouter } from 'next/navigation'
import { HomeDraggableLayer } from './home-draggable-layer'
import { resolveArtUrl } from './stores/config-defaults'
import { useEffect, useRef } from 'react'
import { ImagePlusIcon } from 'lucide-react'
import { toast } from 'sonner'
import { deleteUploadedImage, uploadImage } from '@/lib/file-api'
import { useContentEditStore } from './stores/content-edit-store'

export default function ArtCard() {
	const center = useCenterStore()
	const { cardStyles, siteContent } = useConfigStore()
	const contentEditing = useContentEditStore(state => state.editing)
	const updateSiteContent = useContentEditStore(state => state.updateSiteContent)
	const registerPendingFileId = useContentEditStore(state => state.registerPendingFileId)
	const artInputRef = useRef<HTMLInputElement>(null)
	const latestUploadedArtFileIdRef = useRef<number | null>(null)
	const router = useRouter()
	const styles = cardStyles.artCard
	const hiCardStyles = cardStyles.hiCard

	const x = styles.offsetX !== null ? center.x + styles.offsetX : center.x - styles.width / 2
	const y = styles.offsetY !== null ? center.y + styles.offsetY : center.y - hiCardStyles.height / 2 - styles.height - CARD_SPACING

	const artImages = siteContent.artImages ?? []
	const currentId = siteContent.currentArtImageId
	const currentArt = (currentId ? artImages.find(item => item.id === currentId) : undefined) ?? artImages[0]
	const artUrl = resolveArtUrl(currentArt?.url)

	useEffect(() => {
		if (!contentEditing) {
			latestUploadedArtFileIdRef.current = null
		}
	}, [contentEditing])

	const updateCurrentArt = (patch: Partial<{ url: string; description: string }>) => {
		updateSiteContent(current => {
			const existingImages = current.artImages ?? []
			const selectedId =
				current.currentArtImageId && existingImages.some(item => item.id === current.currentArtImageId) ? current.currentArtImageId : existingImages[0]?.id
			const nextId = selectedId || `url-${Date.now()}`
			const nextImages =
				existingImages.length === 0 || !selectedId
					? [{ id: nextId, url: patch.url ?? resolveArtUrl(undefined), description: patch.description }]
					: existingImages.map(item => (item.id === nextId ? { ...item, ...patch } : item))

			return {
				...current,
				artImages: nextImages,
				currentArtImageId: nextId
			}
		})
	}

	const handleArtFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
		const file = event.target.files?.[0]
		if (!file) return

		try {
			const uploaded = await uploadImage(file, 'site')
			if (latestUploadedArtFileIdRef.current) {
				await deleteUploadedImage(latestUploadedArtFileIdRef.current)
			}
			latestUploadedArtFileIdRef.current = uploaded.fileId
			registerPendingFileId(uploaded.fileId)
			const id = `file-${uploaded.fileId}`
			updateSiteContent(current => {
				const existingImages = current.artImages ?? []
				const filteredImages = existingImages.filter(item => item.id !== id)
				return {
					...current,
					artImages: [...filteredImages, { id, url: uploaded.url }],
					currentArtImageId: id
				}
			})
		} catch (error) {
			toast.error(error instanceof Error ? error.message : '首页图片上传失败')
		} finally {
			event.currentTarget.value = ''
		}
	}

	return (
		<HomeDraggableLayer cardKey='artCard' x={x} y={y} width={styles.width} height={styles.height}>
			<Card className='p-2 max-sm:static max-sm:translate-0' order={styles.order} width={styles.width} height={styles.height} x={x} y={y}>
				{siteContent.enableChristmas && (
					<>
						<img
							src='/images/christmas/snow-3.webp'
							alt='Christmas decoration'
							className='pointer-events-none absolute'
							style={{ width: 160, right: -8, top: -16, opacity: 0.9 }}
						/>
					</>
				)}

				<img
					onClick={() => {
						if (!contentEditing) router.push('/pictures')
					}}
					src={artUrl}
					alt='wall art'
					className={`h-full w-full rounded-[32px] object-cover ${contentEditing ? 'cursor-default' : 'cursor-pointer'}`}
				/>
				{contentEditing && (
					<div
						className='absolute inset-x-5 bottom-5 z-20 rounded-2xl border bg-white/85 p-2 shadow-lg backdrop-blur'
						onClick={event => event.stopPropagation()}>
						<input ref={artInputRef} type='file' accept='image/*' className='hidden' onChange={handleArtFileSelect} />
						<div className='flex items-center gap-2'>
							<button
								type='button'
								title='更换首页图片'
								onClick={() => artInputRef.current?.click()}
								className='bg-brand/15 text-brand flex h-9 w-9 shrink-0 items-center justify-center rounded-full border'>
								<ImagePlusIcon className='h-5 w-5' />
							</button>
							<input
								type='url'
								aria-label='首页图片 URL'
								value={currentArt?.url ?? ''}
								onChange={event => updateCurrentArt({ url: event.target.value })}
								placeholder='图片 URL'
								className='bg-secondary/10 focus:border-brand min-w-0 flex-1 rounded-lg border px-2 py-1.5 text-xs outline-none'
							/>
						</div>
					</div>
				)}
			</Card>
		</HomeDraggableLayer>
	)
}
