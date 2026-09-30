export type Id = string
export interface User { id: Id; name: string; nickname?: string; phone?: string; userType: 'staff' | 'user' }
export interface Page<T> { records: T[]; total: number; current: number; size: number }
export interface Conversation { conversationId: string; title: string; updatedAt?: string }
export interface Reference { documentId?: Id; documentTitle?: string; url?: string; version?: string; chunkContent?: string; similarityScore?: number; retrievalSource?: string; metadata?: Record<string, unknown> }
export interface Message { messageId?: string; type: 'USER' | 'ASSISTANT'; content: string; ragReferences?: Reference[]; helpful?: boolean | null; cacheHit?: boolean; progress?: string; cards?: Record<string, unknown>; error?: string }
export interface KnowledgeDocument { docId: Id; docTitle: string; fileName?: string; fileType?: string; fileUrl?: string; url?: string; status: string; knowledgeBaseType: string; description?: string; accessibleBy?: string; version?: string; currentVersion?: string; lockVersion?: number; createdAt?: string; updatedAt?: string; extension?: string }
export interface DocumentVersion { versionId: Id; version: string; status: string; changelog?: string; createdAt?: string; fileUrl?: string; docUrl?: string }
export interface Segment { id: Id; text: string; chunkOrder: number; status: string; skipEmbedding?: number; metadata?: string; lockVersion?: number }
export interface CacheEntry { id: string; question: string; answer: string; status: string; frequency?: number; hitCount?: number; helpfulCount?: number; unhelpfulCount?: number; createdAt?: string; expiresAt?: string; reviewNote?: string; evidenceJson?: string; sourceRefsJson?: string; [key: string]: unknown }
