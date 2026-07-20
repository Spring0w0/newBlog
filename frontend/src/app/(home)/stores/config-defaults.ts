export type ThemeColors = {
	colorBrand: string
	colorPrimary: string
	colorSecondary: string
	colorBrandSecondary: string
	colorBg: string
	colorBorder: string
	colorCard: string
	colorArticle: string
}

export type SiteContent = {
	meta: {
		title: string
		description: string
		username?: string
	}
	faviconUrl?: string
	avatarUrl?: string
	hiCard?: {
		greeting?: string
		introPrefix?: string
		introSuffix?: string
		avatarLink?: string
	}
	theme: ThemeColors
	backgroundColors: string[]
	artImages?: Array<{ id: string; url: string; description?: string }>
	currentArtImageId?: string
	backgroundImages?: Array<{ id: string; url: string }>
	currentBackgroundImageId?: string
	socialButtons?: Array<{ id: string; type: string; value: string; label?: string; order: number }>
	clockShowSeconds?: boolean
	summaryInContent?: boolean
	isCachePem?: boolean
	hideEditButton?: boolean
	enableCategories?: boolean
	currentHatIndex?: number
	hatFlipped?: boolean
	enableChristmas?: boolean
	beian?: { text: string; link: string }
	[key: string]: unknown
}

export const BUILTIN_AVATAR_URL = '/avatar.png'
export const LEGACY_BUILTIN_AVATAR_URL = '/images/avatar.png'
export const BUILTIN_ART_URL = '/art-cat.png'
export const LEGACY_BUILTIN_ART_URL = '/images/art/cat.png'

export function resolveAvatarUrl(avatarUrl?: string): string {
	const url = avatarUrl?.trim()
	if (!url || url === LEGACY_BUILTIN_AVATAR_URL) return BUILTIN_AVATAR_URL
	return url
}

export function resolveArtUrl(artUrl?: string): string {
	const url = artUrl?.trim()
	if (!url || url === LEGACY_BUILTIN_ART_URL) return BUILTIN_ART_URL
	return url
}

export type CardStyle = {
	width: number
	height: number
	order: number
	offsetX: number | null
	offsetY: number | null
	enabled: boolean
	offset?: number
}

type OffsetCardStyle = CardStyle & { offset: number }

export type CardStyles = {
	[key: string]: CardStyle
	artCard: CardStyle
	hiCard: CardStyle
	clockCard: OffsetCardStyle
	calendarCard: CardStyle
	musicCard: OffsetCardStyle
	socialButtons: CardStyle
	shareCard: CardStyle
	articleCard: CardStyle
	writeButtons: CardStyle
	navCard: CardStyle
	likePosition: CardStyle
	hatCard: CardStyle
	beianCard: CardStyle
}

export const DEFAULT_SITE_CONTENT: SiteContent = {
	meta: {
		title: 'NewBlog',
		description: '个人博客'
	},
	faviconUrl: '/favicon.png',
	avatarUrl: BUILTIN_AVATAR_URL,
	hiCard: {
		greeting: '',
		introPrefix: "I'm",
		introSuffix: 'Nice to meet you!',
		avatarLink: '/live2d'
	},
	theme: {
		colorBrand: '#2fcbe7',
		colorPrimary: '#5B423F',
		colorSecondary: '#8b7667',
		colorBrandSecondary: '#eec25e',
		colorBg: '#d4e8f3',
		colorBorder: '#ffffff',
		colorCard: '#ffffff99',
		colorArticle: '#ffffffcc'
	},
	backgroundColors: ['#f7da3987', '#8fdbe9', '#fffef8'],
	artImages: [],
	currentArtImageId: '',
	backgroundImages: [],
	currentBackgroundImageId: '',
	socialButtons: [],
	clockShowSeconds: false,
	summaryInContent: false,
	isCachePem: false,
	hideEditButton: false,
	enableCategories: true,
	currentHatIndex: 1,
	hatFlipped: false,
	enableChristmas: false,
	beian: { text: '', link: '' }
}

export const DEFAULT_CARD_STYLES: CardStyles = {
	artCard: { width: 360, height: 200, order: 3, offsetX: null, offsetY: null, enabled: true },
	hiCard: { width: 360, height: 288, order: 1, offsetX: null, offsetY: null, enabled: true },
	clockCard: { width: 232, height: 132, offset: 92, order: 4, offsetX: null, offsetY: null, enabled: true },
	calendarCard: { width: 350, height: 286, order: 5, offsetX: null, offsetY: null, enabled: true },
	musicCard: { width: 293, height: 66, offset: 120, order: 6, offsetX: null, offsetY: null, enabled: true },
	socialButtons: { width: 315, height: 48, order: 6, offsetX: null, offsetY: null, enabled: true },
	shareCard: { width: 200, height: 180, order: 7, offsetX: null, offsetY: null, enabled: true },
	articleCard: { width: 266, height: 160, order: 8, offsetX: null, offsetY: null, enabled: true },
	writeButtons: { width: 180, height: 42, order: 8, offsetX: null, offsetY: null, enabled: true },
	navCard: { width: 280, height: 434, order: 2, offsetX: null, offsetY: null, enabled: true },
	likePosition: { width: 54, height: 54, order: 8, offsetX: null, offsetY: null, enabled: true },
	hatCard: { width: 99, height: 105, order: 10, offsetX: -48, offsetY: -168, enabled: false },
	beianCard: { width: 200, height: 60, order: 11, offsetX: null, offsetY: null, enabled: false }
}

const RESET_CARD_STYLES: CardStyles = {
	...DEFAULT_CARD_STYLES,
	hatCard: { width: 120, height: 120, order: 10, offsetX: null, offsetY: null, enabled: false }
}

export function createDefaultSiteContent(): SiteContent {
	return {
		...DEFAULT_SITE_CONTENT,
		meta: { ...DEFAULT_SITE_CONTENT.meta },
		hiCard: { ...DEFAULT_SITE_CONTENT.hiCard },
		theme: { ...DEFAULT_SITE_CONTENT.theme },
		backgroundColors: [...DEFAULT_SITE_CONTENT.backgroundColors],
		artImages: [],
		backgroundImages: [],
		socialButtons: [],
		beian: { ...DEFAULT_SITE_CONTENT.beian! }
	}
}

export function cloneSiteContent(content: SiteContent): SiteContent {
	return {
		...content,
		meta: { ...content.meta },
		hiCard: content.hiCard ? { ...content.hiCard } : undefined,
		theme: { ...content.theme },
		backgroundColors: [...(content.backgroundColors ?? [])],
		artImages: content.artImages?.map(item => ({ ...item })) ?? [],
		backgroundImages: content.backgroundImages?.map(item => ({ ...item })) ?? [],
		socialButtons: content.socialButtons?.map(item => ({ ...item })) ?? [],
		beian: content.beian ? { ...content.beian } : undefined
	}
}

export function createDefaultCardStyles(): CardStyles {
	return cloneCardStyles(DEFAULT_CARD_STYLES)
}

export function createResetCardStyles(): CardStyles {
	return cloneCardStyles(RESET_CARD_STYLES)
}

function cloneCardStyles(cardStyles: CardStyles): CardStyles {
	return Object.fromEntries(Object.entries(cardStyles).map(([key, value]) => [key, { ...value }])) as CardStyles
}
