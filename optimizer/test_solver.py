import pytest
from solver import solve_tsp


class TestSolveTsp:
    """Tests for the TSP solver"""

    # ==================== EDGE CASES ====================

    def test_empty_matrix(self):
        """Empty matrix should return empty route"""
        # GIVEN
        distance_matrix = []

        # WHEN
        route, distance = solve_tsp(distance_matrix)

        # THEN
        assert route == []
        assert distance == 0

    def test_single_location(self):
        """Single location should return just that location"""
        # GIVEN
        distance_matrix = [[0]]

        # WHEN
        route, distance = solve_tsp(distance_matrix)

        # THEN
        assert route == [0]
        assert distance == 0

    # ==================== TWO LOCATIONS ====================

    def test_two_locations_round_trip(self):
        """Two locations round trip: A -> B -> A"""
        # GIVEN
        distance_matrix = [
            [0, 100],   # A to A=0, A to B=100
            [100, 0]    # B to A=100, B to B=0
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix, is_round_trip=True)

        # THEN
        assert route == [0, 1]
        assert distance == 200  # 100 + 100 (there and back)

    def test_two_locations_one_way(self):
        """Two locations one way: A -> B"""
        # GIVEN
        distance_matrix = [
            [0, 100],
            [100, 0]
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix, is_round_trip=False)

        # THEN
        assert route == [0, 1]
        assert distance == 100  # Just A to B

    def test_two_locations_asymmetric(self):
        """Two locations with different distances each way"""
        # GIVEN - A to B is 100, B to A is 150
        distance_matrix = [
            [0, 100],
            [150, 0]
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix, is_round_trip=True)

        # THEN
        assert route == [0, 1]
        assert distance == 250  # 100 + 150

    # ==================== THREE LOCATIONS ====================

    def test_three_locations_obvious_route(self):
        """Three locations where optimal route is obvious: A -> B -> C"""
        # GIVEN - Triangle where A-B-C is clearly shortest
        #     A
        #    /|
        #   B-C
        # A->B=10, B->C=10, A->C=100 (going direct is bad)
        distance_matrix = [
            [0, 10, 100],   # From A
            [10, 0, 10],    # From B
            [100, 10, 0]    # From C
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix, is_round_trip=False)

        # THEN - Should go A -> B -> C (total 20), not A -> C (100)
        assert route == [0, 1, 2]
        assert distance == 20

    def test_three_locations_round_trip(self):
        """Three locations round trip"""
        # GIVEN - equilateral triangle, all distances equal
        distance_matrix = [
            [0, 100, 100],
            [100, 0, 100],
            [100, 100, 0]
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix, is_round_trip=True)

        # THEN - visits all 3, returns to start
        assert len(route) == 3
        assert set(route) == {0, 1, 2}  # All locations visited
        assert distance == 300  # 3 legs of 100 each

    # ==================== ROUTE VALIDITY ====================

    def test_visits_all_locations(self):
        """Should visit every location exactly once"""
        # GIVEN - 5 locations
        distance_matrix = [
            [0, 10, 20, 30, 40],
            [10, 0, 15, 25, 35],
            [20, 15, 0, 12, 22],
            [30, 25, 12, 0, 18],
            [40, 35, 22, 18, 0]
        ]

        # WHEN
        route, distance = solve_tsp(distance_matrix)

        # THEN
        assert len(route) == 5
        assert set(route) == {0, 1, 2, 3, 4}  # All visited
        assert route[0] == 0  # Starts at depot

    def test_starts_at_depot(self):
        """Route should always start at location 0"""
        # GIVEN
        distance_matrix = [
            [0, 50, 30],
            [50, 0, 20],
            [30, 20, 0]
        ]

        # WHEN
        route, _ = solve_tsp(distance_matrix)

        # THEN
        assert route[0] == 0

    # ==================== DISTANCE CALCULATION ====================

    def test_distance_matches_route(self):
        """Total distance should match sum of route segments"""
        # GIVEN
        distance_matrix = [
            [0, 10, 20],
            [10, 0, 15],
            [20, 15, 0]
        ]

        # WHEN
        route, total_distance = solve_tsp(distance_matrix, is_round_trip=False)

        # THEN - manually calculate expected distance
        expected_distance = 0
        for i in range(len(route) - 1):
            expected_distance += distance_matrix[route[i]][route[i + 1]]

        assert total_distance == expected_distance

    def test_round_trip_includes_return(self):
        """Round trip distance should include return to start"""
        # GIVEN - simple path A -> B -> C
        distance_matrix = [
            [0, 10, 100],
            [10, 0, 10],
            [100, 10, 0]
        ]

        # WHEN
        route_one_way, dist_one_way = solve_tsp(distance_matrix, is_round_trip=False)
        route_round, dist_round = solve_tsp(distance_matrix, is_round_trip=True)

        # THEN - round trip should be longer by the return distance
        assert dist_round > dist_one_way
