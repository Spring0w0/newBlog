'use client'

import useSWR from 'swr'
import { getPublic } from '@/lib/public-api'

export function usePublicResource<T>(endpoint: string) {
	return useSWR<T>(endpoint, getPublic<T>, {
		revalidateOnFocus: false,
		revalidateOnReconnect: true
	})
}
