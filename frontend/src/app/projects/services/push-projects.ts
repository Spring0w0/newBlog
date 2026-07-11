import { createAdminProject, deleteAdminProject, getAdminProjects, updateAdminProject, type AdminProject } from '@/lib/admin-content-api'
import type { Project } from '../components/project-card'

export type PushProjectsParams = {
	projects: Project[]
}

export async function pushProjects({ projects }: PushProjectsParams): Promise<AdminProject[]> {
	const existingProjects = await getAdminProjects()
	const persistedIds = new Set(projects.flatMap(project => (project.id === undefined ? [] : [project.id])))

	const savedProjects = await Promise.all(
		projects.map(project => {
			const payload = {
				name: project.name,
				year: project.year,
				description: project.description,
				image: project.image,
				url: project.url,
				tags: project.tags,
				github: project.github,
				npm: project.npm
			}
			return project.id === undefined ? createAdminProject(payload) : updateAdminProject(project.id, payload)
		})
	)

	await Promise.all(existingProjects.filter(project => !persistedIds.has(project.id)).map(project => deleteAdminProject(project.id)))
	return savedProjects
}
