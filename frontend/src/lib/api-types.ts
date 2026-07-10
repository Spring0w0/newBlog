export type ApiCode = number | string

export type ApiQueryValue = string | number | boolean | null | undefined

export type ApiQueryParams = Record<string, ApiQueryValue | ApiQueryValue[]>

export type ApiResponseParseMode = 'json' | 'text' | 'blob' | 'arrayBuffer' | 'response'

export interface ApiResponse<T = unknown> {
	code: ApiCode
	message?: string
	data: T
	success?: boolean
	[key: string]: unknown
}

export interface ApiErrorPayload<T = unknown> {
	code?: ApiCode
	message?: string
	data?: T
	errors?: unknown
	[key: string]: unknown
}

export interface ApiRequestOptions extends Omit<RequestInit, 'body' | 'headers'> {
	baseUrl?: string
	body?: BodyInit | Record<string, unknown> | unknown[] | null
	headers?: HeadersInit
	query?: ApiQueryParams
	auth?: boolean
	timeoutMs?: number
	parseAs?: ApiResponseParseMode
	toastOnError?: boolean
	successCodes?: ApiCode[]
	unwrapResponse?: boolean
	next?: {
		revalidate?: number | false
		tags?: string[]
	}
}

export type ApiFormValue = ApiQueryValue | Blob

export type UploadPayload = FormData | Blob | Blob[] | File | File[] | FileList

export interface ApiUploadOptions extends Omit<ApiRequestOptions, 'body'> {
	fieldName?: string
	fields?: Record<string, ApiFormValue | ApiFormValue[]>
}

export interface UploadedFile {
	url: string
	fileName?: string
	originalName?: string
	size?: number
	contentType?: string
	[key: string]: unknown
}
