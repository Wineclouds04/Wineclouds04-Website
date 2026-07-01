export type MediaAsset = {
  id: number
  originalName: string
  mediaType: string
  extension: string
  sizeBytes: number
  width?: number
  height?: number
  altText?: string
  status: 'PENDING' | 'ACTIVE' | 'ORPHAN' | 'DELETED'
  referenceCount: number
  url: string
  createdAt: string
}

export type MediaPage = {
  items: MediaAsset[]
  total: number
  page: number
  pageSize: number
}
