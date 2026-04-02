export interface DormBuilding {
  id: number;
  name: string;
  address: string | null;
  totalFloors: number;
}

export interface DormRoom {
  id: number;
  dormBuildingId: number | null;
  roomNumber: string;
  floor: number;
  roomType: "SINGLE_SUITE" | "DOUBLE_ROOM";
  pricePerSemester: number;
  capacity: number;
  occupied: number;
  description: string | null;
  hasSpace: boolean;
}

export interface DormApplication {
  id: number;
  status: "DRAFT" | "SUBMITTED" | "APPROVED" | "REJECTED" | "CANCELLED";
  currentStep: number;
  roomTypePreference: "SINGLE_SUITE" | "DOUBLE_ROOM" | null;
  dormRoomId: number | null;
  sleepSchedule: string | null;
  studyEnvironment: string | null;
  preferredRoommateUid: string | null;
  termsAccepted: boolean;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  specialNeeds: string | null;
  createdAt: string;
  updatedAt: string;
}
