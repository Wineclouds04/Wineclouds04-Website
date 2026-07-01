export type ApiClientOptions = {
  baseUrl: string
}

export type ProblemDetail = {
  title?: string
  detail?: string
  status?: number
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly problem?: ProblemDetail
  ) {
    super(problem?.detail ?? problem?.title ?? `API request failed with status ${status}`)
  }
}

const cookie = (name: string) => document.cookie
  .split('; ')
  .find(value => value.startsWith(`${name}=`))
  ?.slice(name.length + 1)

export const createApiClient = ({ baseUrl }: ApiClientOptions) => {
  let accessToken: string | null = null
  let refreshPromise: Promise<boolean> | null = null

  const parse = async <T>(response: Response): Promise<T> => {
    if (response.status === 204) return undefined as T
    if (response.ok) return response.json() as Promise<T>
    let problem: ProblemDetail | undefined
    try {
      problem = await response.json() as ProblemDetail
    } catch {
      // Preserve the HTTP status when an upstream response has no JSON body.
    }
    throw new ApiError(response.status, problem)
  }

  const send = async <T>(
    path: string,
    init: RequestInit = {},
    retry = true
  ): Promise<T> => {
    const headers = new Headers(init.headers)
    if (init.body && !(init.body instanceof FormData)) {
      headers.set('Content-Type', 'application/json')
    }
    if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
    const response = await fetch(`${baseUrl}${path}`, {
      ...init,
      headers,
      credentials: 'include'
    })
    if (
      response.status === 401
      && retry
      && path !== '/auth/login'
      && path !== '/auth/refresh'
      && await refresh()
    ) {
      return send<T>(path, init, false)
    }
    return parse<T>(response)
  }

  const refresh = async (): Promise<boolean> => {
    if (!refreshPromise) {
      refreshPromise = (async () => {
        const csrf = cookie('blog_csrf')
        if (!csrf) return false
        try {
          const response = await fetch(`${baseUrl}/auth/refresh`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'X-CSRF-Token': decodeURIComponent(csrf) }
          })
          if (!response.ok) return false
          const payload = await response.json() as { accessToken: string }
          accessToken = payload.accessToken
          return true
        } catch {
          return false
        }
      })().finally(() => {
        refreshPromise = null
      })
    }
    return refreshPromise
  }

  return {
    setAccessToken(token: string | null) {
      accessToken = token
    },
    refresh,
    get<T>(path: string) {
      return send<T>(path)
    },
    post<T>(path: string, body?: unknown) {
      return send<T>(path, {
        method: 'POST',
        body: body === undefined ? undefined : JSON.stringify(body)
      })
    },
    postForm<T>(path: string, body: FormData) {
      return send<T>(path, {
        method: 'POST',
        body
      })
    },
    put<T>(path: string, body?: unknown) {
      return send<T>(path, {
        method: 'PUT',
        body: body === undefined ? undefined : JSON.stringify(body)
      })
    },
    delete<T>(path: string) {
      return send<T>(path, { method: 'DELETE' })
    },
    async logout() {
      const csrf = cookie('blog_csrf')
      try {
        await send<void>('/auth/logout', {
          method: 'POST',
          headers: csrf ? { 'X-CSRF-Token': decodeURIComponent(csrf) } : {}
        }, false)
      } finally {
        accessToken = null
      }
    }
  }
}
