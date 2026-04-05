// User
export interface User {
  id: number
  username: string
  displayName: string
  role: 'ADMIN' | 'EDITOR'
  status: 'ACTIVE' | 'DISABLED'
  forcePasswordChange: boolean
  createdAt: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

// Category
export interface Category {
  id: number
  parentId: number | null
  name: string
  slug: string
  icon: string | null
  sortOrder: number
  visible: boolean
  createdAt: string
  children?: Category[]
}

// Article
export interface Article {
  id: number
  categoryId: number
  title: string
  slug: string
  draftContent: string | null
  draftContentHtml: string | null
  publishedTitle: string | null
  publishedContentHtml: string | null
  publishedSummary: string | null
  summary: string | null
  status: 'DRAFT' | 'PUBLISHED'
  editorMode: 'RICH' | 'MARKDOWN'
  currentVersion: string | null
  visible: boolean
  deleted: boolean
  sortOrder: number
  authorId: number
  lastEditorId: number | null
  publishedAt: string | null
  updatedAt: string
  createdAt: string
}

export interface PublicArticle {
  id: number
  slug: string
  title: string
  contentHtml: string
  summary: string | null
  currentVersion: string | null
  categoryId: number
  publishedAt: string | null
  updatedAt: string
}

export interface ArticleVersion {
  id: number
  articleId: number
  versionLabel: string
  changeNotes: string | null
  title: string
  content: string
  contentHtml: string
  publishedBy: number
  createdAt: string
}

// Media
export interface Media {
  id: number
  originalName: string
  storedName: string
  filePath: string
  fileSize: number
  contentType: string
  uploadedBy: number
  createdAt: string
}

// Operation Log
export interface OperationLog {
  id: number
  userId: number
  username: string
  displayName: string
  action: string
  targetType: string
  targetId: number
  detail: string | null
  createdAt: string
}

// Pagination
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// Category tree node for visibility management
export interface VisibilityTreeNode extends Category {
  articles?: Array<{
    id: number
    title: string
    status: string
    visible: boolean
  }>
}
