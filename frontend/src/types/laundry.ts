export interface LaundryRoom {
  id: number;
  name: string;
  totalMachines: number;
  dormBuildingId: number | null;
}

export interface LaundryMachine {
  id: number;
  machineNumber: number;
  status: "AVAILABLE" | "IN_USE" | "OUT_OF_ORDER";
}

export interface LaundryRoomAvailability {
  roomId: number;
  roomName: string;
  totalMachines: number;
  availableMachines: number;
  inUse: number;
  outOfOrder: number;
}

export interface LaundryBooking {
  id: number;
  machineId: number | null;
  machineNumber: number;
  status: "BOOKED" | "IN_PROGRESS" | "COMPLETED" | "CANCELLED";
  timeSlotStart: string;
  timeSlotEnd: string;
  createdAt: string;
}
