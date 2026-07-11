import { create } from 'zustand'
import { API_AUTH_ERROR_EVENT } from '@/lib/api-client'
import { clearAllAuthCache, getAuthToken as getToken, getCurrentUser, login as loginWithPassword } from '@/lib/auth'
import { hasAccessToken } from '@/lib/auth-token'
import type { AuthUser, LoginCredentials } from '@/lib/auth'

type AuthSuccessHandler = () => void | Promise<void>

interface AuthStore {
	isAuth: boolean
	user: AuthUser | null
	loginDialogOpen: boolean
	pendingAfterLogin?: AuthSuccessHandler

	clearAuth: () => void
	refreshAuthState: () => void
	getAuthToken: () => Promise<string>
	login: (credentials: LoginCredentials) => Promise<void>
	openLoginDialog: (afterLogin?: AuthSuccessHandler) => void
	closeLoginDialog: () => void
}

export const useAuthStore = create<AuthStore>((set, get) => ({
	isAuth: false,
	user: null,
	loginDialogOpen: false,
	pendingAfterLogin: undefined,

	clearAuth: () => {
		clearAllAuthCache()
		set({ isAuth: false, user: null })
	},

	refreshAuthState: async () => {
		if (!hasAccessToken()) {
			set({ isAuth: false, user: null })
			return
		}

		try {
			const user = await getCurrentUser()
			set({ isAuth: true, user })
		} catch {
			set({ isAuth: false, user: null })
		}
	},

	getAuthToken: async () => {
		const token = await getToken()
		get().refreshAuthState()
		return token
	},

	login: async (credentials: LoginCredentials) => {
		const result = await loginWithPassword(credentials)
		set({ isAuth: true, user: result.user ?? null })
	},

	openLoginDialog: (afterLogin?: AuthSuccessHandler) => {
		set({ loginDialogOpen: true, pendingAfterLogin: afterLogin })
	},

	closeLoginDialog: () => {
		set({ loginDialogOpen: false, pendingAfterLogin: undefined })
	}
}))

void useAuthStore.getState().refreshAuthState()

if (typeof window !== 'undefined') {
	window.addEventListener(API_AUTH_ERROR_EVENT, () => {
		useAuthStore.getState().clearAuth()
		useAuthStore.getState().openLoginDialog()
	})
}
