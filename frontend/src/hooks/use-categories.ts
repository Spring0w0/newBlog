'use client'

import useSWR from 'swr'
import { getPublic } from '@/lib/public-api'

export type CategoriesConfig = {
	categories: string[]
}

const fetcher = async (url: string): Promise<CategoriesConfig> => ({ categories: await getPublic<string[]>(url) })

export function useCategories() {
	const { data, error, isLoading } = useSWR<CategoriesConfig>('/api/categories', fetcher, {
		revalidateOnFocus: false,
		revalidateOnReconnect: true
	})

	return {
		categories: data?.categories ?? [],
		loading: isLoading,
		error
	}
}

