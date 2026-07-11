import { apiClient } from './api-client'

export const IMAGE_UPLOAD_SCOPES = ['blog-images', 'site', 'bloggers', 'projects', 'shares', 'pictures'] as const

export type ImageUploadScope = (typeof IMAGE_UPLOAD_SCOPES)[number]

export type UploadedImage = {
	fileId: number
	url: string
	fileName: string
	originalName: string
	size: number
	contentType: string
}

const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024
const SUPPORTED_IMAGE_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])

export async function uploadImage(file: File, scope: ImageUploadScope): Promise<UploadedImage> {
	validateImageForUpload(file)
	return apiClient.upload<UploadedImage>('/api/admin/files/images', file, {
		fieldName: 'file',
		fields: { scope },
		toastOnError: false
	})
}

export function deleteUploadedImage(fileId: number): Promise<void> {
	return apiClient.delete<void>(`/api/admin/files/${fileId}`, { toastOnError: false })
}

export function validateImageForUpload(file: File): void {
	if (!SUPPORTED_IMAGE_TYPES.has(file.type)) {
		throw new Error('仅支持 JPEG、PNG、GIF 或 WebP 图片')
	}
	if (file.size <= 0) {
		throw new Error('图片文件不能为空')
	}
	if (file.size > MAX_IMAGE_SIZE_BYTES) {
		throw new Error('单个图片不能超过 10MB')
	}
}
