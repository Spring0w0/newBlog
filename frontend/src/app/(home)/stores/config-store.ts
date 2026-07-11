import { create } from 'zustand'
import { getPublic } from '@/lib/public-api'
import { createDefaultCardStyles, createDefaultSiteContent, type CardStyles, type SiteContent } from './config-defaults'

export type { CardStyles, SiteContent } from './config-defaults'

interface ConfigStore {
	siteContent: SiteContent
	cardStyles: CardStyles
	regenerateKey: number
	configDialogOpen: boolean
	setSiteContent: (content: SiteContent) => void
	setCardStyles: (styles: CardStyles) => void
	resetSiteContent: () => void
	resetCardStyles: () => void
	refreshPublicConfig: () => Promise<void>
	regenerateBubbles: () => void
	setConfigDialogOpen: (open: boolean) => void
}

export const useConfigStore = create<ConfigStore>((set, get) => ({
	siteContent: createDefaultSiteContent(),
	cardStyles: createDefaultCardStyles(),
	regenerateKey: 0,
	configDialogOpen: false,
	setSiteContent: (content: SiteContent) => {
		set({ siteContent: content })
	},
	setCardStyles: (styles: CardStyles) => {
		set({ cardStyles: styles })
	},
	resetSiteContent: () => {
		set({ siteContent: createDefaultSiteContent() })
	},
	resetCardStyles: () => {
		set({ cardStyles: createDefaultCardStyles() })
	},
	refreshPublicConfig: async () => {
		const [nextSiteContent, nextCardStyles] = await Promise.all([
			getPublic<SiteContent>('/api/site/config'),
			getPublic<CardStyles>('/api/site/card-styles')
		])
		set({ siteContent: nextSiteContent, cardStyles: nextCardStyles })
	},
	regenerateBubbles: () => {
		set(state => ({ regenerateKey: state.regenerateKey + 1 }))
	},
	setConfigDialogOpen: (open: boolean) => {
		set({ configDialogOpen: open })
	}
}))

