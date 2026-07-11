import { get } from './api-client'

/**
 * 公开内容请求不携带登录凭证，避免访客读取受到过期 Token 影响。
 */
export function getPublic<T>(endpoint: string): Promise<T> {
	return get<T>(endpoint, { auth: false })
}
