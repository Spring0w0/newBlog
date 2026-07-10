import { API_BASE_URL } from '@/consts'
import { clearAuthTokens, getAccessToken } from './auth-token'
import type {
	ApiCode,
	ApiErrorPayload,
	ApiFormValue,
	ApiQueryParams,
	ApiQueryValue,
	ApiRequestOptions,
	ApiResponse,
	ApiResponseParseMode,
	ApiUploadOptions,
	UploadPayload
} from './api-types'

export const API_AUTH_ERROR_EVENT = 'newblog:api-auth-error'

const DEFAULT_SUCCESS_CODES: ApiCode[] = [0, 200, '0', '200', 'ok', 'OK', 'success', 'SUCCESS']
const AUTH_ERROR_CODES: ApiCode[] = [401, 403, '401', '403', 'unauthorized', 'UNAUTHORIZED', 'forbidden', 'FORBIDDEN']

type UploadFilePayload = Exclude<UploadPayload, FormData>

export class ApiError<T = unknown> extends Error {
	status?: number
	code?: ApiCode
	data?: T
	response?: Response

	constructor(message: string, options: { status?: number; code?: ApiCode; data?: T; response?: Response; cause?: unknown } = {}) {
		super(message)
		this.name = 'ApiError'
		this.status = options.status
		this.code = options.code
		this.data = options.data
		this.response = options.response
		Object.setPrototypeOf(this, ApiError.prototype)

		if (options.cause) {
			;(this as Error & { cause?: unknown }).cause = options.cause
		}
	}
}

export function isApiError(error: unknown): error is ApiError {
	return error instanceof ApiError
}

export function buildApiUrl(endpoint: string, query?: ApiQueryParams, baseUrl: string = API_BASE_URL): string {
	const joinedUrl = joinBaseAndEndpoint(baseUrl, endpoint)
	return appendQuery(joinedUrl, query)
}

export async function request<T = unknown>(endpoint: string, options: ApiRequestOptions = {}): Promise<T> {
	const {
		baseUrl,
		body,
		headers: initHeaders,
		query,
		auth = true,
		timeoutMs,
		parseAs = 'json',
		toastOnError = false,
		successCodes = DEFAULT_SUCCESS_CODES,
		unwrapResponse = true,
		method,
		signal,
		...fetchOptions
	} = options

	const headers = new Headers(initHeaders)
	const requestBody = prepareRequestBody(body, headers)
	const controller = timeoutMs || signal ? new AbortController() : null
	let timeoutId: ReturnType<typeof setTimeout> | undefined

	if (signal) {
		if (signal.aborted) {
			controller?.abort(signal.reason)
		} else {
			signal.addEventListener('abort', () => controller?.abort(signal.reason), { once: true })
		}
	}

	if (timeoutMs && controller) {
		timeoutId = setTimeout(() => controller.abort(new Error('Request timeout')), timeoutMs)
	}

	try {
		if (!headers.has('Accept') && parseAs === 'json') {
			headers.set('Accept', 'application/json')
		}

		if (auth && !headers.has('Authorization')) {
			const token = getAccessToken()
			if (token) headers.set('Authorization', `Bearer ${token}`)
		}

		const response = await fetch(buildApiUrl(endpoint, query, baseUrl), {
			...fetchOptions,
			method: method ?? (requestBody ? 'POST' : 'GET'),
			headers,
			body: requestBody,
			signal: controller?.signal ?? signal
		})
		const payload = await parseResponseBody(response, parseAs)

		if (!response.ok) {
			throw createHttpError(response, payload)
		}

		if (parseAs !== 'json' || !unwrapResponse) {
			return payload as T
		}

		return unwrapApiResponse<T>(payload, response, successCodes)
	} catch (error) {
		const apiError = normalizeRequestError(error)
		await handleApiError(apiError, toastOnError)
		throw apiError
	} finally {
		if (timeoutId) clearTimeout(timeoutId)
	}
}

export function get<T = unknown>(endpoint: string, options: Omit<ApiRequestOptions, 'body' | 'method'> = {}): Promise<T> {
	return request<T>(endpoint, { ...options, method: 'GET' })
}

export function post<T = unknown>(endpoint: string, body?: ApiRequestOptions['body'], options: Omit<ApiRequestOptions, 'body' | 'method'> = {}): Promise<T> {
	return request<T>(endpoint, { ...options, method: 'POST', body })
}

export function put<T = unknown>(endpoint: string, body?: ApiRequestOptions['body'], options: Omit<ApiRequestOptions, 'body' | 'method'> = {}): Promise<T> {
	return request<T>(endpoint, { ...options, method: 'PUT', body })
}

export function patch<T = unknown>(endpoint: string, body?: ApiRequestOptions['body'], options: Omit<ApiRequestOptions, 'body' | 'method'> = {}): Promise<T> {
	return request<T>(endpoint, { ...options, method: 'PATCH', body })
}

export function del<T = unknown>(endpoint: string, options: Omit<ApiRequestOptions, 'body' | 'method'> = {}): Promise<T> {
	return request<T>(endpoint, { ...options, method: 'DELETE' })
}

export function upload<T = unknown>(endpoint: string, payload: UploadPayload, options: ApiUploadOptions = {}): Promise<T> {
	const { fieldName = 'file', fields, method = 'POST', ...requestOptions } = options
	const formData = createUploadFormData(payload, fieldName)
	appendUploadFields(formData, fields)
	return request<T>(endpoint, { ...requestOptions, method, body: formData })
}

export const apiClient = {
	request,
	get,
	post,
	put,
	patch,
	delete: del,
	upload
}

function joinBaseAndEndpoint(baseUrl: string | undefined, endpoint: string): string {
	const cleanEndpoint = endpoint.trim()
	if (isAbsoluteUrl(cleanEndpoint)) return cleanEndpoint

	const cleanBase = (baseUrl ?? '').trim().replace(/\/+$/, '')
	const endpointWithSlash = cleanEndpoint.startsWith('/') ? cleanEndpoint : `/${cleanEndpoint}`
	if (!cleanBase) return endpointWithSlash

	const normalizedEndpoint = /\/api$/i.test(cleanBase) && endpointWithSlash.startsWith('/api/') ? endpointWithSlash.slice(4) : endpointWithSlash
	return `${cleanBase}${normalizedEndpoint}`
}

function appendQuery(url: string, query?: ApiQueryParams): string {
	if (!query) return url

	const isAbsolute = isAbsoluteUrl(url)
	const parserBaseUrl = 'http://newblog.local'
	const parsedUrl = new URL(url, isAbsolute ? undefined : parserBaseUrl)

	for (const [key, value] of Object.entries(query)) {
		if (Array.isArray(value)) {
			for (const item of value) appendSearchParam(parsedUrl, key, item)
		} else {
			appendSearchParam(parsedUrl, key, value)
		}
	}

	if (isAbsolute) return parsedUrl.toString()
	return `${parsedUrl.pathname}${parsedUrl.search}${parsedUrl.hash}`
}

function appendSearchParam(url: URL, key: string, value: ApiQueryValue): void {
	if (value === null || value === undefined) return
	url.searchParams.append(key, String(value))
}

function isAbsoluteUrl(url: string): boolean {
	return /^[a-z][a-z\d+\-.]*:\/\//i.test(url)
}

function prepareRequestBody(body: ApiRequestOptions['body'], headers: Headers): BodyInit | undefined {
	if (body === null || body === undefined) return undefined
	if (isBodyInit(body)) return body

	if (!headers.has('Content-Type')) {
		headers.set('Content-Type', 'application/json')
	}

	return JSON.stringify(body)
}

function isBodyInit(body: unknown): body is BodyInit {
	if (typeof body === 'string') return true
	if (typeof URLSearchParams !== 'undefined' && body instanceof URLSearchParams) return true
	if (typeof FormData !== 'undefined' && body instanceof FormData) return true
	if (typeof Blob !== 'undefined' && body instanceof Blob) return true
	if (typeof ArrayBuffer !== 'undefined' && body instanceof ArrayBuffer) return true
	if (ArrayBuffer.isView(body)) return true
	if (typeof ReadableStream !== 'undefined' && body instanceof ReadableStream) return true
	return false
}

async function parseResponseBody(response: Response, parseAs: ApiResponseParseMode): Promise<unknown> {
	if (parseAs === 'response') return response
	if (response.status === 204 || response.status === 205) return undefined
	if (parseAs === 'blob') return response.blob()
	if (parseAs === 'arrayBuffer') return response.arrayBuffer()

	const text = await response.text()
	if (parseAs === 'text') return text
	if (!text) return undefined

	try {
		return JSON.parse(text)
	} catch {
		return text
	}
}

function unwrapApiResponse<T>(payload: unknown, response: Response, successCodes: ApiCode[]): T {
	if (!isApiResponse(payload)) return payload as T

	if (payload.success === true || isSuccessCode(payload.code, successCodes)) {
		return payload.data as T
	}

	throw new ApiError(getPayloadMessage(payload) ?? 'API request failed', {
		status: response.status,
		code: payload.code,
		data: payload.data,
		response
	})
}

function createHttpError(response: Response, payload: unknown): ApiError {
	const errorPayload = isRecord(payload) ? (payload as ApiErrorPayload) : undefined
	return new ApiError(getPayloadMessage(errorPayload) ?? response.statusText ?? 'API request failed', {
		status: response.status,
		code: errorPayload?.code ?? response.status,
		data: errorPayload?.data ?? payload,
		response
	})
}

function normalizeRequestError(error: unknown): ApiError {
	if (isApiError(error)) return error

	if (typeof DOMException !== 'undefined' && error instanceof DOMException && error.name === 'AbortError') {
		return new ApiError('Request was cancelled or timed out', { status: 0, cause: error })
	}

	if (error instanceof Error) {
		return new ApiError(error.message || 'Network request failed', { status: 0, cause: error })
	}

	return new ApiError('Network request failed', { status: 0, data: error })
}

async function handleApiError(error: ApiError, toastOnError: boolean): Promise<void> {
	if (isAuthError(error)) {
		clearAuthTokens()
		dispatchAuthErrorEvent(error)
	}

	if (toastOnError && typeof window !== 'undefined') {
		const { toast } = await import('sonner')
		toast.error(error.message)
	}
}

function isAuthError(error: ApiError): boolean {
	if (error.status === 401 || error.status === 403) return true
	return error.code !== undefined && AUTH_ERROR_CODES.some(code => String(code).toLowerCase() === String(error.code).toLowerCase())
}

function dispatchAuthErrorEvent(error: ApiError): void {
	if (typeof window === 'undefined') return
	window.dispatchEvent(
		new CustomEvent(API_AUTH_ERROR_EVENT, {
			detail: {
				status: error.status,
				code: error.code,
				message: error.message
			}
		})
	)
}

function isSuccessCode(code: ApiCode, successCodes: ApiCode[]): boolean {
	return successCodes.some(successCode => String(successCode).toLowerCase() === String(code).toLowerCase())
}

function isApiResponse(payload: unknown): payload is ApiResponse {
	return isRecord(payload) && 'code' in payload && ('data' in payload || 'message' in payload)
}

function isRecord(value: unknown): value is Record<string, unknown> {
	return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function getPayloadMessage(payload?: ApiErrorPayload | ApiResponse): string | undefined {
	return typeof payload?.message === 'string' && payload.message.trim() ? payload.message : undefined
}

function createUploadFormData(payload: UploadPayload, fieldName: string): FormData {
	if (typeof FormData === 'undefined') {
		throw new ApiError('File upload is not supported in this runtime')
	}

	if (payload instanceof FormData) return payload

	const formData = new FormData()
	const files = normalizeUploadPayload(payload)
	for (const file of files) {
		formData.append(fieldName, file)
	}
	return formData
}

function normalizeUploadPayload(payload: UploadFilePayload): Blob[] {
	if (Array.isArray(payload)) return payload
	if (isFileList(payload)) return Array.from(payload)
	return [payload]
}

function isFileList(payload: unknown): payload is FileList {
	return typeof FileList !== 'undefined' && payload instanceof FileList
}

function appendUploadFields(formData: FormData, fields?: ApiUploadOptions['fields']): void {
	if (!fields) return

	for (const [key, value] of Object.entries(fields)) {
		if (Array.isArray(value)) {
			for (const item of value) appendUploadField(formData, key, item)
		} else {
			appendUploadField(formData, key, value)
		}
	}
}

function appendUploadField(formData: FormData, key: string, value: ApiFormValue): void {
	if (value === null || value === undefined) return
	formData.append(key, typeof Blob !== 'undefined' && value instanceof Blob ? value : String(value))
}
