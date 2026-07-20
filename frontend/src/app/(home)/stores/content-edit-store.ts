'use client'

import { create } from 'zustand'
import { deleteUploadedImage } from '@/lib/file-api'
import { pushSiteContent } from '../services/push-site-content'
import { useConfigStore, type SiteContent } from './config-store'
import { cloneSiteContent } from './config-defaults'

type SiteContentUpdater = SiteContent | ((current: SiteContent) => SiteContent)

interface ContentEditState {
	editing: boolean
	isSaving: boolean
	snapshot: SiteContent | null
	pendingFileIds: number[]
	startEditing: () => void
	cancelEditing: () => Promise<void>
	saveEditing: () => Promise<void>
	updateSiteContent: (updater: SiteContentUpdater) => void
	registerPendingFileId: (fileId: number) => void
}

export const useContentEditStore = create<ContentEditState>((set, get) => ({
	editing: false,
	isSaving: false,
	snapshot: null,
	pendingFileIds: [],
	startEditing: () => {
		const { siteContent } = useConfigStore.getState()
		set({
			editing: true,
			snapshot: cloneSiteContent(siteContent),
			pendingFileIds: []
		})
	},
	cancelEditing: async () => {
		const { snapshot, pendingFileIds } = get()
		if (snapshot) {
			useConfigStore.getState().setSiteContent(cloneSiteContent(snapshot))
		}

		if (pendingFileIds.length > 0) {
			await Promise.allSettled(pendingFileIds.map(fileId => deleteUploadedImage(fileId)))
		}

		set({
			editing: false,
			isSaving: false,
			snapshot: null,
			pendingFileIds: []
		})
	},
	saveEditing: async () => {
		set({ isSaving: true })
		try {
			const { siteContent, cardStyles, setSiteContent, setCardStyles, refreshPublicConfig } = useConfigStore.getState()
			const saved = await pushSiteContent(siteContent, cardStyles)
			setSiteContent(saved.config)
			setCardStyles(saved.cardStyles)
			await refreshPublicConfig()
			set({
				editing: false,
				isSaving: false,
				snapshot: null,
				pendingFileIds: []
			})
		} catch (error) {
			set({ isSaving: false })
			throw error
		}
	},
	updateSiteContent: updater => {
		const { siteContent, setSiteContent } = useConfigStore.getState()
		const nextContent = typeof updater === 'function' ? updater(cloneSiteContent(siteContent)) : updater
		setSiteContent(nextContent)
	},
	registerPendingFileId: fileId => {
		set(state => ({
			pendingFileIds: state.pendingFileIds.includes(fileId) ? state.pendingFileIds : [...state.pendingFileIds, fileId]
		}))
	}
}))
