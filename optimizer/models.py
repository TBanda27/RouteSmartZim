from pydantic import BaseModel
from typing import List, Optional


class Location(BaseModel):
    name: str
    latitude: float
    longitude: float
    original_input: Optional[str] = None
    input_type: Optional[str] = None


class OptimizationRequest(BaseModel):
    locations: List[Location]
    distance_matrix: List[List[int]]  # distances in meters
    is_round_trip: bool = True


class OptimizationResponse(BaseModel):
    optimized_order: List[int]  # indices of locations in optimized order
    optimized_locations: List[Location]
    total_distance_meters: int
    total_distance_km: float
    route_description: List[str]  # human-readable route steps
