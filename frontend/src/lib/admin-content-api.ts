import { del, get, post, put } from './api-client'

export type AboutContent = {
	title: string
	description: string
	content: string
}

export function saveAdminAbout(content: AboutContent): Promise<AboutContent> {
	return put<AboutContent>('/api/admin/about', content)
}

export function saveAdminSnippets(items: string[]): Promise<string[]> {
	return put<string[]>('/api/admin/snippets', { items })
}

export type AdminBlogger = {
	id: number
	name: string
	avatar: string
	url: string
	description: string
	stars: number
	status: 'recent' | 'disconnected'
}

export type BloggerPayload = Omit<AdminBlogger, 'id'>

export const getAdminBloggers = () => get<AdminBlogger[]>('/api/admin/bloggers')
export const createAdminBlogger = (payload: BloggerPayload) => post<AdminBlogger>('/api/admin/bloggers', payload)
export const updateAdminBlogger = (id: number, payload: BloggerPayload) => put<AdminBlogger>(`/api/admin/bloggers/${id}`, payload)
export const deleteAdminBlogger = (id: number) => del<void>(`/api/admin/bloggers/${id}`)

export type AdminProject = {
	id: number
	name: string
	year: number
	description: string
	image: string
	url: string
	tags: string[]
	github?: string
	npm?: string
}

export type ProjectPayload = Omit<AdminProject, 'id'>

export const getAdminProjects = () => get<AdminProject[]>('/api/admin/projects')
export const createAdminProject = (payload: ProjectPayload) => post<AdminProject>('/api/admin/projects', payload)
export const updateAdminProject = (id: number, payload: ProjectPayload) => put<AdminProject>(`/api/admin/projects/${id}`, payload)
export const deleteAdminProject = (id: number) => del<void>(`/api/admin/projects/${id}`)

export type AdminShare = {
	id: number
	name: string
	logo: string
	url: string
	description: string
	tags: string[]
	stars: number
}

export type SharePayload = Omit<AdminShare, 'id'>

export const getAdminShares = () => get<AdminShare[]>('/api/admin/shares')
export const createAdminShare = (payload: SharePayload) => post<AdminShare>('/api/admin/shares', payload)
export const updateAdminShare = (id: number, payload: SharePayload) => put<AdminShare>(`/api/admin/shares/${id}`, payload)
export const deleteAdminShare = (id: number) => del<void>(`/api/admin/shares/${id}`)

export type AdminPicture = {
	id: string
	uploadedAt: string
	description?: string
	images: string[]
}

export type PicturePayload = {
	uploadedAt?: string
	description?: string
	images: string[]
}

export const getAdminPictures = () => get<AdminPicture[]>('/api/admin/pictures')
export const createAdminPicture = (payload: PicturePayload) => post<AdminPicture>('/api/admin/pictures', payload)
export const updateAdminPicture = (id: string, payload: PicturePayload) => put<AdminPicture>(`/api/admin/pictures/${id}`, payload)
export const deleteAdminPicture = (id: string) => del<void>(`/api/admin/pictures/${id}`)
