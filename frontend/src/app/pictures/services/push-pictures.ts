import { createAdminPicture, deleteAdminPicture, getAdminPictures, updateAdminPicture, type AdminPicture } from '@/lib/admin-content-api'
import type { Picture } from '../page'

export type PushPicturesParams = {
	pictures: Picture[]
}

export async function pushPictures({ pictures }: PushPicturesParams): Promise<AdminPicture[]> {
	const existingPictures = await getAdminPictures()
	const existingIds = new Set(existingPictures.map(picture => picture.id))
	const persistedIds = new Set(pictures.filter(picture => existingIds.has(picture.id)).map(picture => picture.id))

	const savedPictures = await Promise.all(
		pictures.map(picture => {
			const payload = {
				uploadedAt: normalizeLocalDateTime(picture.uploadedAt),
				description: picture.description,
				images: picture.images && picture.images.length > 0 ? picture.images : picture.image ? [picture.image] : []
			}
			return existingIds.has(picture.id) ? updateAdminPicture(picture.id, payload) : createAdminPicture(payload)
		})
	)

	await Promise.all(existingPictures.filter(picture => !persistedIds.has(picture.id)).map(picture => deleteAdminPicture(picture.id)))
	return savedPictures
}

function normalizeLocalDateTime(value: string | undefined): string | undefined {
	if (!value) return undefined
	return value.endsWith('Z') ? value.slice(0, -1) : value
}
