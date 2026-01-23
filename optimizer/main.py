from fastapi import FastAPI, HTTPException
from models import OptimizationRequest, OptimizationResponse, Location
from solver import solve_tsp
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(
    title="RouteSmart Optimizer",
    description="TSP optimization service using Google OR-Tools",
    version="1.0.0"
)


@app.get("/health")
def health_check():
    return {"status": "healthy", "service": "optimizer"}


@app.post("/optimize", response_model=OptimizationResponse)
def optimize_route(request: OptimizationRequest):
    logger.info(f"Received optimization request for {len(request.locations)} locations")

    # Validate input
    num_locations = len(request.locations)
    if num_locations < 2:
        raise HTTPException(status_code=400, detail="At least 2 locations required")

    if num_locations > 10:
        raise HTTPException(status_code=400, detail="Maximum 10 locations allowed")

    matrix_size = len(request.distance_matrix)
    if matrix_size != num_locations:
        raise HTTPException(
            status_code=400,
            detail=f"Distance matrix size ({matrix_size}) doesn't match locations ({num_locations})"
        )

    # Solve TSP
    optimized_order, total_distance = solve_tsp(
        request.distance_matrix,
        request.is_round_trip
    )

    # Build optimized locations list
    optimized_locations = [request.locations[i] for i in optimized_order]

    # Build route description
    route_description = []
    for i, idx in enumerate(optimized_order):
        loc = request.locations[idx]
        if i == 0:
            route_description.append(f"Start at {loc.name}")
        else:
            prev_idx = optimized_order[i - 1]
            distance_km = request.distance_matrix[prev_idx][idx] / 1000.0
            route_description.append(f"Go to {loc.name} ({distance_km:.2f} km)")

    if request.is_round_trip and len(optimized_order) > 0:
        first_idx = optimized_order[0]
        last_idx = optimized_order[-1]
        return_distance_km = request.distance_matrix[last_idx][first_idx] / 1000.0
        route_description.append(f"Return to {request.locations[first_idx].name} ({return_distance_km:.2f} km)")

    total_distance_km = total_distance / 1000.0

    logger.info(f"Optimization complete: {total_distance_km:.2f} km total")

    return OptimizationResponse(
        optimized_order=optimized_order,
        optimized_locations=optimized_locations,
        total_distance_meters=total_distance,
        total_distance_km=round(total_distance_km, 2),
        route_description=route_description
    )


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8001)
