import { createAdminShare, deleteAdminShare, getAdminShares, updateAdminShare, type AdminShare } from '@/lib/admin-content-api'
import type { Share } from '../components/share-card'

export type PushSharesParams = {
	shares: Share[]
}

export async function pushShares({ shares }: PushSharesParams): Promise<AdminShare[]> {
	const existingShares = await getAdminShares()
	const persistedIds = new Set(shares.flatMap(share => (share.id === undefined ? [] : [share.id])))

	const savedShares = await Promise.all(
		shares.map(share => {
			const payload = {
				name: share.name,
				logo: share.logo,
				url: share.url,
				description: share.description,
				tags: share.tags,
				stars: share.stars
			}
			return share.id === undefined ? createAdminShare(payload) : updateAdminShare(share.id, payload)
		})
	)

	await Promise.all(existingShares.filter(share => !persistedIds.has(share.id)).map(share => deleteAdminShare(share.id)))
	return savedShares
}
