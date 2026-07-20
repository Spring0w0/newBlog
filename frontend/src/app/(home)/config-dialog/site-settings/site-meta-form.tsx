'use client'

import type { SiteContent } from '../../stores/config-store'

interface SiteMetaFormProps {
	formData: SiteContent
	setFormData: React.Dispatch<React.SetStateAction<SiteContent>>
}

export function SiteMetaForm({ formData, setFormData }: SiteMetaFormProps) {
	const updateHiCard = (patch: NonNullable<SiteContent['hiCard']>) => {
		setFormData({
			...formData,
			hiCard: {
				...formData.hiCard,
				...patch
			}
		})
	}

	return (
		<>
			<div className='grid grid-cols-2 gap-2'>
				<div>
					<label className='mb-2 block text-sm font-medium'>站点标题</label>
					<input
						type='text'
						value={formData.meta.title}
						onChange={e => setFormData({ ...formData, meta: { ...formData.meta, title: e.target.value } })}
						className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
					/>
				</div>

				<div>
					<label className='mb-2 block text-sm font-medium'>用户名</label>
					<input
						type='text'
						value={formData.meta.username || ''}
						onChange={e => setFormData({ ...formData, meta: { ...formData.meta, username: e.target.value } })}
						className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
					/>
				</div>
			</div>

			<div>
				<label className='mb-2 block text-sm font-medium'>站点描述</label>
				<textarea
					value={formData.meta.description}
					onChange={e => setFormData({ ...formData, meta: { ...formData.meta, description: e.target.value } })}
					rows={3}
					className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
				/>
			</div>

			<div className='rounded-xl border bg-white/40 p-4'>
				<div className='mb-3 text-sm font-medium'>首页问候卡片</div>
				<div className='grid grid-cols-2 gap-2'>
					<div>
						<label className='mb-2 block text-xs font-medium text-gray-500'>问候语</label>
						<input
							type='text'
							value={formData.hiCard?.greeting ?? ''}
							onChange={e => updateHiCard({ greeting: e.target.value })}
							placeholder='留空时按当前时间自动显示'
							className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
						/>
					</div>
					<div>
						<label className='mb-2 block text-xs font-medium text-gray-500'>头像点击链接</label>
						<input
							type='text'
							value={formData.hiCard?.avatarLink ?? ''}
							onChange={e => updateHiCard({ avatarLink: e.target.value })}
							placeholder='/live2d'
							className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
						/>
					</div>
					<div>
						<label className='mb-2 block text-xs font-medium text-gray-500'>用户名前缀</label>
						<input
							type='text'
							value={formData.hiCard?.introPrefix ?? ''}
							onChange={e => updateHiCard({ introPrefix: e.target.value })}
							placeholder="I'm"
							className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
						/>
					</div>
					<div>
						<label className='mb-2 block text-xs font-medium text-gray-500'>问候结尾</label>
						<input
							type='text'
							value={formData.hiCard?.introSuffix ?? ''}
							onChange={e => updateHiCard({ introSuffix: e.target.value })}
							placeholder='Nice to meet you!'
							className='bg-secondary/10 w-full rounded-lg border px-4 py-2 text-sm'
						/>
					</div>
				</div>
			</div>
		</>
	)
}
