export type AuthTokenStorage = 'local' | 'session'

export interface SaveAuthTokenOptions {
	storage?: AuthTokenStorage
	remember?: boolean
}

export interface AuthTokens {
	accessToken?: string | null
	refreshToken?: string | null
}

const ACCESS_TOKEN_KEY = 'newblog.access_token'
const REFRESH_TOKEN_KEY = 'newblog.refresh_token'
const DEFAULT_STORAGE: AuthTokenStorage = 'local'

function getBrowserStorage(storage: AuthTokenStorage): Storage | null {
	if (typeof window === 'undefined') return null

	try {
		return storage === 'local' ? window.localStorage : window.sessionStorage
	} catch {
		return null
	}
}

function resolveStorage(options?: SaveAuthTokenOptions): AuthTokenStorage {
	if (options?.storage) return options.storage
	if (options?.remember === false) return 'session'
	return DEFAULT_STORAGE
}

function readToken(key: string): string | null {
	return readStorageToken('local', key) ?? readStorageToken('session', key)
}

function readStorageToken(storage: AuthTokenStorage, key: string): string | null {
	const target = getBrowserStorage(storage)
	if (!target) return null

	try {
		return target.getItem(key)
	} catch {
		return null
	}
}

function writeToken(key: string, token: string, options?: SaveAuthTokenOptions): void {
	const storage = resolveStorage(options)
	const target = getBrowserStorage(storage)
	if (!target) return

	try {
		target.setItem(key, token)
		removeStorageToken(storage === 'local' ? 'session' : 'local', key)
	} catch {
		// Storage can fail in private mode or when quota is exhausted.
	}
}

function removeStorageToken(storage: AuthTokenStorage, key: string): void {
	const target = getBrowserStorage(storage)
	if (!target) return

	try {
		target.removeItem(key)
	} catch {
		// ignore
	}
}

function removeToken(key: string): void {
	removeStorageToken('local', key)
	removeStorageToken('session', key)
}

export function getAccessToken(): string | null {
	return readToken(ACCESS_TOKEN_KEY)
}

export function saveAccessToken(token: string, options?: SaveAuthTokenOptions): void {
	writeToken(ACCESS_TOKEN_KEY, token, options)
}

export function clearAccessToken(): void {
	removeToken(ACCESS_TOKEN_KEY)
}

export function getRefreshToken(): string | null {
	return readToken(REFRESH_TOKEN_KEY)
}

export function saveRefreshToken(token: string, options?: SaveAuthTokenOptions): void {
	writeToken(REFRESH_TOKEN_KEY, token, options)
}

export function clearRefreshToken(): void {
	removeToken(REFRESH_TOKEN_KEY)
}

export function saveAuthTokens(tokens: AuthTokens, options?: SaveAuthTokenOptions): void {
	if (tokens.accessToken) saveAccessToken(tokens.accessToken, options)
	if (tokens.refreshToken) saveRefreshToken(tokens.refreshToken, options)
}

export function clearAuthTokens(): void {
	clearAccessToken()
	clearRefreshToken()
}

export function hasAccessToken(): boolean {
	return !!getAccessToken()
}

export function getBearerToken(): string | null {
	const token = getAccessToken()
	return token ? `Bearer ${token}` : null
}

export function getAuthorizationHeader(): Record<string, string> {
	const bearerToken = getBearerToken()
	return bearerToken ? { Authorization: bearerToken } : {}
}

export const getAuthToken = getAccessToken
export const saveAuthToken = saveAccessToken
export const clearAuthToken = clearAccessToken
