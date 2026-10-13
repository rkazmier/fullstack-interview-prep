// Types mirroring the backend DTOs (API contract)

export type TaskStatus = 'TODO' | 'IN_PROGRESS' | 'DONE';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface TaskResponse {
  id: number;
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  ownerUsername: string;
  tags: string[];
  createdAt: string;
  updatedAt: string | null;
}

export interface TaskRequest {
  title: string;
  description: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  tags: string[];
}

export interface TaskStats {
  status: TaskStatus;
  count: number;
}

// Spring Data Page<T> shape
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number; // current page index (0-based)
  size: number;
  first: boolean;
  last: boolean;
}

export interface AuthResponse {
  token: string;
  username: string;
  role: string;
}

export interface ApiError {
  status: number;
  message: string;
  fieldErrors: Record<string, string>;
  timestamp: string;
}
