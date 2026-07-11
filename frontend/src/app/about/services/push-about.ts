import { saveAdminAbout } from '@/lib/admin-content-api'

export type AboutData = {
	title: string
	description: string
	content: string
}

export async function pushAbout(data: AboutData): Promise<AboutData> {
	return saveAdminAbout({
		title: data.title.trim(),
		description: data.description.trim(),
		content: data.content
	})
}
