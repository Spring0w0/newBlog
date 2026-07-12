import { get, request, ApiError } from './api-client'
import { clearAuthTokens, getAccessToken, hasAccessToken, saveAuthTokens } from './auth-token'

export interface LoginCredentials {
	username: string
	password: string
	remember?: boolean
}

export interface AuthUser {
	id?: string | number
	username?: string
	nickname?: string
	role?: string
	roles?: string[]
	[key: string]: unknown
}

export interface LoginResult {
	accessToken: string
	refreshToken?: string
	user?: AuthUser
}

type RawLoginResponse =
	| string
	| {
			accessToken?: string
			access_token?: string
			token?: string
			jwt?: string
			refreshToken?: string
			refresh_token?: string
			user?: AuthUser
			account?: AuthUser
			admin?: AuthUser
			[key: string]: unknown
	  }

export async function login(credentials: LoginCredentials): Promise<LoginResult> {
	const response = await request<RawLoginResponse>('/api/auth/login', {
		method: 'POST',
		body: {
			username: credentials.username,
			password: credentials.password
		},
		auth: false,
		toastOnError: false
	})
	const result = normalizeLoginResponse(response)

	saveAuthTokens(
		{
			accessToken: result.accessToken,
			refreshToken: result.refreshToken
		},
		{ remember: credentials.remember }
	)

	return {
		...result,
		user: result.user ?? (await getCurrentUser())
	}
}

export function clearAllAuthCache(): void {
	clearAuthTokens()
}

export async function hasAuth(): Promise<boolean> {
	if (!hasAccessToken()) return false
	try {
		await getCurrentUser()
		return true
	} catch {
		return false
	}
}

export function getCurrentUser(): Promise<AuthUser> {
	return get<AuthUser>('/api/auth/me', { toastOnError: false })
}

export async function getAuthToken(): Promise<string> {
	const token = getAccessToken()
	if (!token) {
		throw new ApiError('Please login first', { status: 401, code: 401 })
	}
	return token
}

function normalizeLoginResponse(response: RawLoginResponse): LoginResult {
	if (typeof response === 'string') {
		const accessToken = response.trim()
		if (!accessToken) throw new ApiError('Login response is missing access token')
		return { accessToken }
	}

	const accessToken = response.accessToken ?? response.access_token ?? response.token ?? response.jwt
	if (!accessToken || typeof accessToken !== 'string') {
		throw new ApiError('Login response is missing access token')
	}

	const refreshToken = response.refreshToken ?? response.refresh_token
	const user = response.user ?? response.account ?? response.admin

	return {
		accessToken,
		refreshToken: typeof refreshToken === 'string' ? refreshToken : undefined,
		user
	}
}
