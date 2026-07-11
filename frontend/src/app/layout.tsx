import '@/styles/globals.css'

import type { Metadata } from 'next'
import Layout from '@/layout'
import Head from '@/layout/head'
import { DEFAULT_SITE_CONTENT, type SiteContent } from '@/app/(home)/stores/config-defaults'
import { getPublicSiteUrl, getServerSiteConfig } from '@/lib/server-public-api'

async function loadSiteConfig(): Promise<SiteContent> {
	try {
		return await getServerSiteConfig()
	} catch (error) {
		console.warn('读取站点 metadata 配置失败，使用内置默认值：', error)
		return DEFAULT_SITE_CONTENT
	}
}

export async function generateMetadata(): Promise<Metadata> {
	const siteContent = await loadSiteConfig()
	const title = siteContent.meta.title
	const description = siteContent.meta.description
	return {
		metadataBase: new URL(getPublicSiteUrl()),
		title,
		description,
		icons: siteContent.faviconUrl ? { icon: siteContent.faviconUrl } : undefined,
		openGraph: {
			title,
			description,
			type: 'website',
			url: '/'
		},
		twitter: {
			card: 'summary',
			title,
			description
		}
	}
}

export default async function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
	const siteContent = await loadSiteConfig()
	const { theme } = siteContent
	const htmlStyle = {
		cursor: 'url(/images/cursor.svg) 2 1, auto',
		'--color-brand': theme.colorBrand,
		'--color-primary': theme.colorPrimary,
		'--color-secondary': theme.colorSecondary,
		'--color-brand-secondary': theme.colorBrandSecondary,
		'--color-bg': theme.colorBg,
		'--color-border': theme.colorBorder,
		'--color-card': theme.colorCard,
		'--color-article': theme.colorArticle
	} as React.CSSProperties

	return (
		<html lang='zh-CN' suppressHydrationWarning style={htmlStyle}>
			<Head />

			<body>
				<script
					dangerouslySetInnerHTML={{
						__html: `
					if (/windows|win32/i.test(navigator.userAgent)) {
						document.documentElement.classList.add('windows');
					}
			      `
					}}
				/>

				<Layout>{children}</Layout>
			</body>
		</html>
	)
}
