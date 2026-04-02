export interface FoodCategory {
  id: number;
  name: string;
  icon: string | null;
  sortOrder: number;
}

export interface FoodItem {
  id: number;
  categoryId: number | null;
  name: string;
  description: string | null;
  price: number;
  imageUrl: string | null;
  popular: boolean;
}

export interface FoodOrderItem {
  id: number;
  foodItemId: number | null;
  foodItemName: string | null;
  quantity: number;
  unitPrice: number;
}

export interface FoodOrder {
  id: number;
  status: "PENDING" | "CONFIRMED" | "READY" | "PICKED_UP" | "CANCELLED";
  totalAmount: number;
  items: FoodOrderItem[];
  note: string | null;
  pickupTime: string | null;
  createdAt: string;
  updatedAt: string;
}
