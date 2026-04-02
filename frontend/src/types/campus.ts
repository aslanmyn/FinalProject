export interface CampusBuilding {
  id: number;
  name: string;
  code: string | null;
  description: string | null;
  buildingType: string | null;
  latitude: number | null;
  longitude: number | null;
  floorCount: number;
  imageUrl: string | null;
}

export interface CampusRoom {
  id: number;
  buildingId: number | null;
  buildingName: string | null;
  roomNumber: string;
  floor: number;
  roomType: string | null;
  name: string | null;
  description: string | null;
  capacity: number | null;
  latitude: number | null;
  longitude: number | null;
}

export interface NavigationEdge {
  id: number;
  fromRoomId: number | null;
  toRoomId: number | null;
  fromBuildingId: number | null;
  toBuildingId: number | null;
  distanceMeters: number;
  accessible: boolean;
}

export interface NavigationResult {
  edges: NavigationEdge[];
  totalDistanceMeters: number;
}
