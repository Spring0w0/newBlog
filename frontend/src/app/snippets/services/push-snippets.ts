import { saveAdminSnippets } from '@/lib/admin-content-api'

export type PushSnippetsParams = {
	snippets: string[]
}

export async function pushSnippets({ snippets }: PushSnippetsParams): Promise<string[]> {
	return saveAdminSnippets(Array.from(new Set(snippets.map(snippet => snippet.trim()).filter(Boolean))))
}
