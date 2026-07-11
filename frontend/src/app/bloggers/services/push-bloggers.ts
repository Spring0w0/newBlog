import { createAdminBlogger, deleteAdminBlogger, getAdminBloggers, updateAdminBlogger, type AdminBlogger } from '@/lib/admin-content-api'
import type { Blogger } from '../grid-view'

export type PushBloggersParams = {
	bloggers: Blogger[]
}

export async function pushBloggers({ bloggers }: PushBloggersParams): Promise<AdminBlogger[]> {
	const existingBloggers = await getAdminBloggers()
	const persistedIds = new Set(bloggers.flatMap(blogger => (blogger.id === undefined ? [] : [blogger.id])))

	const savedBloggers = await Promise.all(
		bloggers.map(blogger => {
			const payload = {
				name: blogger.name,
				avatar: blogger.avatar,
				url: blogger.url,
				description: blogger.description,
				stars: blogger.stars,
				status: blogger.status ?? 'recent'
			}
			return blogger.id === undefined ? createAdminBlogger(payload) : updateAdminBlogger(blogger.id, payload)
		})
	)

	await Promise.all(existingBloggers.filter(blogger => !persistedIds.has(blogger.id)).map(blogger => deleteAdminBlogger(blogger.id)))
	return savedBloggers
}
