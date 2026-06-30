export type ApiClientOptions = {
  baseUrl: string
}

export const createApiClient = ({ baseUrl }: ApiClientOptions) => ({
  async get<T>(path: string): Promise<T> {
    const response = await fetch(`${baseUrl}${path}`)
    if (!response.ok) {
      throw new Error(`API request failed with status ${response.status}`)
    }
    return response.json() as Promise<T>
  }
})

