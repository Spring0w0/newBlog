import { useCenterStore } from '@/hooks/use-center'
import Card from '@/components/card'
import { useConfigStore } from './stores/config-store'
import { HomeDraggableLayer } from './home-draggable-layer'
import Link from 'next/link'
import { resolveAvatarUrl } from './stores/config-defaults'
import { useEffect, useRef } from 'react'
import { ImagePlusIcon } from 'lucide-react'
import { toast } from 'sonner'
import { deleteUploadedImage, uploadImage } from '@/lib/file-api'
import { useContentEditStore } from './stores/content-edit-store'

function getGreeting() {
	const hour = new Date().getHours()

	if (hour >= 6 && hour < 12) {
		return 'Good Morning'
	} else if (hour >= 12 && hour < 18) {
		return 'Good Afternoon'
	} else if (hour >= 18 && hour < 22) {
		return 'Good Evening'
	} else {
		return 'Good Night'
	}
}

export default function HiCard() {
	const center = useCenterStore()
	const { cardStyles, siteContent } = useConfigStore()
	const contentEditing = useContentEditStore(state => state.editing)
	const updateSiteContent = useContentEditStore(state => state.updateSiteContent)
	const registerPendingFileId = useContentEditStore(state => state.registerPendingFileId)
	const avatarInputRef = useRef<HTMLInputElement>(null)
	const latestUploadedAvatarFileIdRef = useRef<number | null>(null)
	const styles = cardStyles.hiCard
	const greeting = siteContent.hiCard?.greeting?.trim() || getGreeting()
	const username = siteContent.meta.username?.trim() || 'Suni'
	const introPrefix = siteContent.hiCard?.introPrefix ?? "I'm"
	const introSuffix = siteContent.hiCard?.introSuffix ?? 'Nice to meet you!'
	const avatarUrl = resolveAvatarUrl(siteContent.avatarUrl)
	const avatarLink = normalizeAvatarLink(siteContent.hiCard?.avatarLink)
	const avatarImage = (
		<img src={avatarUrl} alt='avatar' className='mx-auto h-[120px] w-[120px] rounded-full object-cover' style={{ boxShadow: ' 0 16px 32px -5px #E2D9CE' }} />
	)

	const updateHiCard = (patch: NonNullable<typeof siteContent.hiCard>) => {
		updateSiteContent(current => ({
			...current,
			hiCard: {
				...current.hiCard,
				...patch
			}
		}))
	}

	const updateUsername = (value: string) => {
		updateSiteContent(current => ({
			...current,
			meta: {
				...current.meta,
				username: value
			}
		}))
	}

	const handleAvatarFileSelect = async (event: React.ChangeEvent<HTMLInputElement>) => {
		const file = event.target.files?.[0]
		if (!file) return

		try {
			const uploaded = await uploadImage(file, 'site')
			if (latestUploadedAvatarFileIdRef.current) {
				await deleteUploadedImage(latestUploadedAvatarFileIdRef.current)
			}
			latestUploadedAvatarFileIdRef.current = uploaded.fileId
			registerPendingFileId(uploaded.fileId)
			updateSiteContent(current => ({
				...current,
				avatarUrl: uploaded.url
			}))
		} catch (error) {
			toast.error(error instanceof Error ? error.message : '头像上传失败')
		} finally {
			event.currentTarget.value = ''
		}
	}

	const x = styles.offsetX !== null ? center.x + styles.offsetX : center.x - styles.width / 2
	const y = styles.offsetY !== null ? center.y + styles.offsetY : center.y - styles.height / 2

	useEffect(() => {
		if (!contentEditing) {
			latestUploadedAvatarFileIdRef.current = null
		}
	}, [contentEditing])

	return (
		<HomeDraggableLayer cardKey='hiCard' x={x} y={y} width={styles.width} height={styles.height}>
			<Card order={styles.order} width={styles.width} height={styles.height} x={x} y={y} className='relative text-center max-sm:static max-sm:translate-0'>
				{siteContent.enableChristmas && (
					<>
						<img
							src='/images/christmas/snow-1.webp'
							alt='Christmas decoration'
							className='pointer-events-none absolute'
							style={{ width: 180, left: -20, top: -25, opacity: 0.9 }}
						/>
						<img
							src='/images/christmas/snow-2.webp'
							alt='Christmas decoration'
							className='pointer-events-none absolute'
							style={{ width: 160, bottom: -12, right: -8, opacity: 0.9 }}
						/>
					</>
				)}
				{contentEditing ? (
					<div className='relative mx-auto h-[120px] w-[120px]'>
						<input ref={avatarInputRef} type='file' accept='image/*' className='hidden' onChange={handleAvatarFileSelect} />
						{avatarImage}
						<button
							type='button'
							title='更换头像'
							onClick={() => avatarInputRef.current?.click()}
							className='absolute inset-0 flex items-center justify-center rounded-full bg-black/35 text-white opacity-0 transition-opacity hover:opacity-100'>
							<ImagePlusIcon className='h-7 w-7' />
						</button>
					</div>
				) : isExternalLink(avatarLink) ? (
					<a href={avatarLink} target='_blank' rel='noreferrer'>
						{avatarImage}
					</a>
				) : (
					<Link href={avatarLink}>{avatarImage}</Link>
				)}
				{contentEditing ? (
					<div className='font-averia mx-auto mt-3 flex max-w-[300px] flex-col items-center gap-1 px-6 text-center'>
						<input
							type='text'
							aria-label='问候语'
							value={siteContent.hiCard?.greeting ?? ''}
							onChange={event => updateHiCard({ greeting: event.target.value })}
							placeholder={getGreeting()}
							className='focus:border-brand w-full rounded-lg border bg-white/60 px-2 py-1 text-center text-2xl leading-tight outline-none'
						/>
						<div className='grid w-full grid-cols-[76px_1fr] gap-1'>
							<input
								type='text'
								aria-label='用户名前缀'
								value={introPrefix}
								onChange={event => updateHiCard({ introPrefix: event.target.value })}
								className='focus:border-brand rounded-lg border bg-white/60 px-2 py-1 text-center text-xl outline-none'
							/>
							<input
								type='text'
								aria-label='用户名'
								value={siteContent.meta.username ?? ''}
								onChange={event => updateUsername(event.target.value)}
								placeholder='Suni'
								className='text-brand focus:border-brand rounded-lg border bg-white/60 px-2 py-1 text-center text-[32px] leading-none outline-none'
							/>
						</div>
						<textarea
							aria-label='问候结尾'
							value={introSuffix}
							onChange={event => updateHiCard({ introSuffix: event.target.value })}
							rows={2}
							className='focus:border-brand w-full resize-none rounded-lg border bg-white/60 px-2 py-1 text-center text-xl leading-tight outline-none'
						/>
						<input
							type='text'
							aria-label='头像点击链接'
							value={siteContent.hiCard?.avatarLink ?? ''}
							onChange={event => updateHiCard({ avatarLink: event.target.value })}
							placeholder='/live2d'
							className='text-secondary focus:border-brand w-full rounded-lg border bg-white/60 px-2 py-1 text-center text-xs outline-none'
						/>
					</div>
				) : (
					<h1 className='font-averia mx-auto mt-3 max-w-[300px] px-5 text-2xl leading-tight [overflow-wrap:anywhere] break-words'>
						{greeting} <br />
						{introPrefix && <>{introPrefix} </>}
						<span className='text-linear text-[32px]'>{username}</span>
						{introSuffix && <> , {introSuffix}</>}
					</h1>
				)}
			</Card>
		</HomeDraggableLayer>
	)
}

function normalizeAvatarLink(value?: string): string {
	const link = value?.trim()
	if (!link) return '/live2d'
	if (link.startsWith('/') || isExternalLink(link)) return link
	return '/live2d'
}

function isExternalLink(value: string): boolean {
	return /^https?:\/\//i.test(value)
}
