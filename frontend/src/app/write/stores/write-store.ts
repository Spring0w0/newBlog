import { create } from 'zustand'
import { toast } from 'sonner'
import { hashFileSHA256 } from '@/lib/file-utils'
import { getAdminBlogBySlug } from '@/lib/admin-blog-api'
import { deleteUploadedImage, uploadImage, validateImageForUpload } from '@/lib/file-api'
import type { PublishForm, ImageItem } from '../types'

export const formatDateTimeLocal = (date: Date = new Date()): string => {
	const pad = (n: number) => String(n).padStart(2, '0')
	const year = date.getFullYear()
	const month = pad(date.getMonth() + 1)
	const day = pad(date.getDate())
	const hours = pad(date.getHours())
	const minutes = pad(date.getMinutes())
	return `${year}-${month}-${day}T${hours}:${minutes}`
}

type WriteStore = {
	// Mode state
	mode: 'create' | 'edit'
	originalSlug: string | null
	originalId: number | null
	setMode: (mode: 'create' | 'edit', originalSlug?: string, originalId?: number) => void

	// Form state
	form: PublishForm
	updateForm: (updates: Partial<PublishForm>) => void
	setForm: (form: PublishForm) => void

	// Image state
	images: ImageItem[]
	addUrlImage: (url: string) => ImageItem
	addFiles: (files: FileList | File[]) => Promise<ImageItem[]>
	uploadFiles: (files: FileList | File[]) => Promise<ImageItem[]>
	deleteImage: (id: string) => Promise<void>

	// Cover state
	cover: ImageItem | null
	setCover: (cover: ImageItem | null) => void

	// Publish state
	loading: boolean
	setLoading: (loading: boolean) => void

	// Load blog for editing
	loadBlogForEdit: (slug: string) => Promise<void>

	// Reset to create mode
	reset: () => void
}

const initialForm: PublishForm = {
	slug: '',
	title: '',
	md: '',
	tags: [],
	date: formatDateTimeLocal(),
	summary: '',
	hidden: false,
	category: ''
}

export const useWriteStore = create<WriteStore>((set, get) => ({
	// Mode state
	mode: 'create',
	originalSlug: null,
	originalId: null,
	setMode: (mode, originalSlug, originalId) => set({ mode, originalSlug: originalSlug || null, originalId: originalId || null }),

	// Form state
	form: { ...initialForm },
	updateForm: updates => set(state => ({ form: { ...state.form, ...updates } })),
	setForm: form => set({ form }),

	// Image state
	images: [],
	addUrlImage: url => {
		const { images } = get()
		const existing = images.find(it => it.type === 'url' && it.url === url)
		if (existing) {
			toast.info('该图片已在列表中')
			return existing
		}
		const id = Math.random().toString(36).slice(2, 10)
		const image: ImageItem = { id, type: 'url', url }
		set(state => ({ images: [image, ...state.images] }))
		return image
	},
	addFiles: async (files: FileList | File[]) => {
		const { images } = get()
		const arr = Array.from(files).filter(f => f.type.startsWith('image/'))
		if (arr.length === 0) return []

		const existingHashes = new Map<string, ImageItem>(
			images
				.filter((it): it is Extract<ImageItem, { type: 'file'; hash?: string }> => it.type === 'file' && (it as any).hash)
				.map(it => [(it as any).hash as string, it])
		)

		const computed = await Promise.all(
			arr.map(async file => {
				const hash = await hashFileSHA256(file)
				return { file, hash }
			})
		)

		const seen = new Set<string>()
		const unique = computed.filter(({ hash }) => {
			if (existingHashes.has(hash)) return false
			if (seen.has(hash)) return false
			seen.add(hash)
			return true
		})

		const resultImages: ImageItem[] = []

		// 处理已存在的图片
		for (const { hash } of computed) {
			if (existingHashes.has(hash)) {
				resultImages.push(existingHashes.get(hash)!)
			}
		}

		// 处理新图片
		if (unique.length > 0) {
			const newItems: ImageItem[] = unique.map(({ file, hash }) => {
				const id = Math.random().toString(36).slice(2, 10)
				const previewUrl = URL.createObjectURL(file)
				const filename = file.name
				return { id, type: 'file', file, previewUrl, filename, hash }
			})

			set(state => ({ images: [...newItems, ...state.images] }))
			resultImages.push(...newItems)
		} else if (resultImages.length === 0) {
			toast.info('图片已存在，不重复添加')
		}

		return resultImages
	},
	uploadFiles: async files => {
		const candidates = Array.from(files)
		if (candidates.length === 0) return []

		try {
			for (const file of candidates) validateImageForUpload(file)

			const { images } = get()
			const existingHashes = new Map<string, ImageItem>(
				images.filter(image => image.hash).map(image => [image.hash as string, image])
			)
			const computed = await Promise.all(
				candidates.map(async file => ({ file, hash: await hashFileSHA256(file) }))
			)
			const seen = new Set<string>()
			const resultImages: ImageItem[] = []
			const pendingUploads = computed.filter(({ hash }) => {
				const existing = existingHashes.get(hash)
				if (existing) {
					resultImages.push(existing)
					return false
				}
				if (seen.has(hash)) return false
				seen.add(hash)
				return true
			})

			for (const { file, hash } of pendingUploads) {
				const uploaded = await uploadImage(file, 'blog-images')
				resultImages.push({
					id: Math.random().toString(36).slice(2, 10),
					type: 'url',
					url: uploaded.url,
					fileId: uploaded.fileId,
					filename: uploaded.fileName,
					hash
				})
			}

			const newImages = resultImages.filter(image => !images.some(existing => existing.id === image.id))
			if (newImages.length > 0) {
				set(state => ({ images: [...newImages, ...state.images] }))
			}
			if (pendingUploads.length > 0) {
				toast.success(`已上传 ${pendingUploads.length} 张图片`)
			} else {
				toast.info('图片已在列表中，不会重复上传')
			}
			return resultImages
		} catch (error) {
			const message = error instanceof Error ? error.message : '图片上传失败'
			toast.error(message)
			throw error
		}
	},
	deleteImage: async id => {
		const image = get().images.find(item => item.id === id)
		if (!image) return
		const markdownReference = image.type === 'url' ? image.url : `local-image:${image.id}`
		if (get().form.md.includes(`(${markdownReference})`)) {
			toast.error('图片仍在文章正文中使用，请先移除 Markdown 引用')
			return
		}

		try {
			if (image.type === 'url' && image.fileId) {
				await deleteUploadedImage(image.fileId)
			}
			if (image.type === 'file') {
				URL.revokeObjectURL(image.previewUrl)
			}
			set(state => ({
				images: state.images.filter(item => item.id !== id),
				cover: state.cover?.id === id ? null : state.cover
			}))
			toast.success('图片已删除')
		} catch (error) {
			const message = error instanceof Error ? error.message : '图片删除失败'
			toast.error(message)
			throw error
		}
	},

	// Cover state
	cover: null,
	setCover: cover => set({ cover }),

	// Publish state
	loading: false,
	setLoading: loading => set({ loading }),

	// Load blog for editing
	loadBlogForEdit: async (slug: string) => {
		try {
			set({ loading: true })
			const blog = await getAdminBlogBySlug(slug)

			// 数据库记录保存了正文图片引用；额外从 Markdown 补齐历史文章尚未建立的外部图片。
			const images: ImageItem[] = []
			for (const url of blog.imageUrls) {
				if (url && url !== blog.cover && !images.some(image => image.type === 'url' && image.url === url)) {
					images.push({ id: Math.random().toString(36).slice(2, 10), type: 'url', url })
				}
			}
			const imageRegex = /!\[.*?\]\((.*?)\)/g
			let match
			while ((match = imageRegex.exec(blog.markdown)) !== null) {
				const url = match[1]
				// 跳过封面，只收集正文图片。
				if (url && url !== blog.cover && !url.startsWith('local-image:')) {
					if (!images.some(img => img.type === 'url' && img.url === url)) {
						const id = Math.random().toString(36).slice(2, 10)
						images.push({ id, type: 'url', url })
					}
				}
			}

			// Set cover
			let cover: ImageItem | null = null
			if (blog.cover) {
				const coverId = Math.random().toString(36).slice(2, 10)
				cover = { id: coverId, type: 'url', url: blog.cover }
			}

			// Set form
			set({
				mode: 'edit',
				originalSlug: slug,
				originalId: blog.id,
				form: {
					slug,
					title: blog.title || '',
					md: blog.markdown,
					tags: blog.tags || [],
					date: blog.publishedAt ? formatDateTimeLocal(new Date(blog.publishedAt)) : formatDateTimeLocal(),
					summary: blog.summary || '',
					hidden: blog.hidden,
					category: blog.category || ''
				},
				images,
				cover,
				loading: false
			})

			toast.success('博客加载成功')
		} catch (err: any) {
			console.error('Failed to load blog:', err)
			toast.error(err?.message || '加载博客失败')
			set({ loading: false })
			throw err
		}
	},

	// Reset to create mode
	reset: () => {
		// Revoke object URLs
		const { images, cover } = get()
		for (const img of images) {
			if (img.type === 'file') {
				URL.revokeObjectURL(img.previewUrl)
			}
		}
		if (cover?.type === 'file') {
			URL.revokeObjectURL(cover.previewUrl)
		}

		set({
			mode: 'create',
			originalSlug: null,
			originalId: null,
			form: { ...initialForm, date: formatDateTimeLocal() },
			images: [],
			cover: null
		})
	}
}))
