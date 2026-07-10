'use client'

import { FormEvent, useEffect, useState } from 'react'
import { Lock, LogIn, User, X } from 'lucide-react'
import { toast } from 'sonner'
import { DialogModal } from '@/components/dialog-modal'
import { useAuthStore } from '@/hooks/use-auth'

export function AuthLoginDialog() {
	const loginDialogOpen = useAuthStore(state => state.loginDialogOpen)
	const closeLoginDialog = useAuthStore(state => state.closeLoginDialog)
	const login = useAuthStore(state => state.login)
	const [username, setUsername] = useState('')
	const [password, setPassword] = useState('')
	const [remember, setRemember] = useState(true)
	const [submitting, setSubmitting] = useState(false)

	useEffect(() => {
		if (loginDialogOpen) {
			setPassword('')
		}
	}, [loginDialogOpen])

	const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
		event.preventDefault()
		const normalizedUsername = username.trim()

		if (!normalizedUsername || !password) {
			toast.error('请输入账号和密码')
			return
		}

		try {
			setSubmitting(true)
			await login({ username: normalizedUsername, password, remember })
			const afterLogin = useAuthStore.getState().pendingAfterLogin
			closeLoginDialog()
			toast.success('登录成功')
			await afterLogin?.()
		} catch (error: any) {
			console.error(error)
			toast.error(error?.message || '登录失败')
		} finally {
			setSubmitting(false)
		}
	}

	return (
		<DialogModal open={loginDialogOpen} onClose={closeLoginDialog} className='card w-[420px] max-w-full p-6'>
			<form onSubmit={handleSubmit} className='space-y-5'>
				<div className='flex items-start justify-between gap-4'>
					<div>
						<h2 className='text-xl font-semibold'>管理登录</h2>
					</div>
					<button type='button' onClick={closeLoginDialog} className='rounded-full p-1 text-gray-400 transition-colors hover:bg-gray-100 hover:text-gray-700'>
						<X className='h-4 w-4' />
					</button>
				</div>

				<label className='block space-y-2 text-sm'>
					<span className='text-secondary'>账号</span>
					<div className='flex items-center gap-2 rounded-xl border bg-white/80 px-3 py-2'>
						<User className='text-secondary h-4 w-4' />
						<input
							value={username}
							onChange={event => setUsername(event.target.value)}
							autoComplete='username'
							className='min-w-0 flex-1 bg-transparent outline-none'
							placeholder='username'
						/>
					</div>
				</label>

				<label className='block space-y-2 text-sm'>
					<span className='text-secondary'>密码</span>
					<div className='flex items-center gap-2 rounded-xl border bg-white/80 px-3 py-2'>
						<Lock className='text-secondary h-4 w-4' />
						<input
							type='password'
							value={password}
							onChange={event => setPassword(event.target.value)}
							autoComplete='current-password'
							className='min-w-0 flex-1 bg-transparent outline-none'
							placeholder='password'
						/>
					</div>
				</label>

				<label className='text-secondary flex items-center gap-2 text-sm'>
					<input type='checkbox' checked={remember} onChange={event => setRemember(event.target.checked)} className='h-4 w-4 accent-[var(--color-brand)]' />
					保持登录状态
				</label>

				<button type='submit' disabled={submitting} className='brand-btn flex w-full items-center justify-center gap-2 px-4 py-2 disabled:opacity-70'>
					<LogIn className='h-4 w-4' />
					{submitting ? '登录中...' : '登录'}
				</button>
			</form>
		</DialogModal>
	)
}
