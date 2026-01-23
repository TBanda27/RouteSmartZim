from ortools.constraint_solver import routing_enums_pb2
from ortools.constraint_solver import pywrapcp
from typing import List, Tuple
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def solve_tsp(distance_matrix: List[List[int]], is_round_trip: bool = True) -> Tuple[List[int], int]:
    """
    Solve the Traveling Salesman Problem using Google OR-Tools.

    Args:
        distance_matrix: 2D list of distances in meters between locations
        is_round_trip: If True, return to starting point

    Returns:
        Tuple of (optimized_order, total_distance)
    """
    num_locations = len(distance_matrix)

    if num_locations <= 1:
        return [0] if num_locations == 1 else [], 0

    if num_locations == 2:
        distance = distance_matrix[0][1]
        if is_round_trip:
            distance += distance_matrix[1][0]
        return [0, 1], distance

    # Create the routing index manager
    manager = pywrapcp.RoutingIndexManager(
        num_locations,  # number of nodes
        1,              # number of vehicles
        0               # depot (starting point)
    )

    # Create the routing model
    routing = pywrapcp.RoutingModel(manager)

    # Create the distance callback
    def distance_callback(from_index, to_index):
        from_node = manager.IndexToNode(from_index)
        to_node = manager.IndexToNode(to_index)
        return distance_matrix[from_node][to_node]

    transit_callback_index = routing.RegisterTransitCallback(distance_callback)

    # Define cost of each arc
    routing.SetArcCostEvaluatorOfAllVehicles(transit_callback_index)

    # Set search parameters
    search_parameters = pywrapcp.DefaultRoutingSearchParameters()
    search_parameters.first_solution_strategy = (
        routing_enums_pb2.FirstSolutionStrategy.PATH_CHEAPEST_ARC
    )
    search_parameters.local_search_metaheuristic = (
        routing_enums_pb2.LocalSearchMetaheuristic.GUIDED_LOCAL_SEARCH
    )
    search_parameters.time_limit.seconds = 5  # 5 second time limit

    # Solve the problem
    logger.info(f"Solving TSP for {num_locations} locations, round_trip={is_round_trip}")
    solution = routing.SolveWithParameters(search_parameters)

    if solution:
        # Extract the route
        route = []
        total_distance = 0
        index = routing.Start(0)

        while not routing.IsEnd(index):
            node = manager.IndexToNode(index)
            route.append(node)
            previous_index = index
            index = solution.Value(routing.NextVar(index))
            total_distance += routing.GetArcCostForVehicle(previous_index, index, 0)

        # For non-round trips, don't count the return to depot
        if not is_round_trip:
            # Recalculate distance without return leg
            total_distance = 0
            for i in range(len(route) - 1):
                total_distance += distance_matrix[route[i]][route[i + 1]]

        logger.info(f"Solution found: {route}, total_distance={total_distance}m")
        return route, total_distance
    else:
        logger.error("No solution found!")
        # Return original order if no solution found
        return list(range(num_locations)), 0
