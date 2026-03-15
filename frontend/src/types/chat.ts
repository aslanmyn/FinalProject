export interface ChatRoom {
  id: number;
  name: string;
  type: "SECTION" | "DIRECT" | "GROUP";
  sectionId: number | null;
  createdAt: string;
}

export interface ChatMessageItem {
  id: number;
  roomId: number;
  senderId: number;
  senderName: string;
  senderRole: string;
  content: string;
  createdAt: string;
}

export interface ChatMember {
  id: number;
  name: string;
  role: string;
}

export interface ChatUserResult {
  id: number;
  name: string;
  role: string;
}

export interface ChatMessagePage {
  items: ChatMessageItem[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}
