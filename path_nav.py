import heapq
import fiona
import random
import csv
import os
from typing import Dict, List, Tuple

_tick_time_mins = 0.25
_over_break_p = 0.1
OVER_BREAK_SPEED_REDUCTION = 0.1 # 10%
ACCELERATION = 10 * _tick_time_mins # Increase in speed in km/h per tick

#TODO:
# - Implement walking as mode of transport
# - Report n.o. walking evacuating, n.o. vehicle evacuating
# - Report total evacuated

def set_tick_time_mins(t):
    _tick_time_mins = t
    
def set_over_break_p(p):
    _over_break_p = p

def kmhToTickSpeed(kmh_speed):
    return (kmh_speed / 60) * _tick_time_mins

def tickSpeedToKmh(tick_speed):
    return (tick_speed / _tick_time_mins) * 60 

class RoadGraph:
    def __init__(self):
        """
        Initialize an empty graph using an adjacency list representation.
        The graph is stored as a dictionary where:
        - Keys are nodes
        - Values are dictionaries of neighboring nodes and their edge weights
        """
        self.nodes = {}
        self.road_data = {} #Key = (a,b) where road connects a,b #Value = {cars,max_speed}
        self.ideal_speeds = {} # Track ideal speed for each road segment
        with fiona.open("./data/SW_RoadLink.shp") as shapefile:
            for road in shapefile:
                # Add graph edges
                start = road.properties["startNode"]
                end = road.properties["endNode"]
                length = road.properties["length"] / 1000 #Road length in km rather than m
                self.__add_edge(start,end,length)
                self.__add_edge(end,start,length)
                # Add speed limit data
                roadType = road.properties["formOfWay"]
                if roadType == "Single Carriageway":
                    max_speed = kmhToTickSpeed(96) # Single Carriageway => 60mph, 96 km/h
                elif roadType == "Dual Carriageway":
                    max_speed = kmhToTickSpeed(112) # Dual Carriageway => 70mph, 112 km/h
                else:
                    max_speed = kmhToTickSpeed(32) # Default 20mph speed limit as 32km/h
                self.road_data[(start,end)]["max_speed"] = max_speed #km per tick
                self.road_data[(end,start)]["max_speed"] = max_speed #km per tick   
                self.ideal_speeds[(start,end)] = max_speed
                self.ideal_speeds[(end,start)] = max_speed
                
    def setEvacNode(self, node_id):
        self.evacNode = node_id

    def calculatePaths(self):
        distances, prev_nodes = self.__dijkstra(self.evacNode)
        self.prev_nodes = prev_nodes
    
    def getPathFromNode(self, node_id):
        return self.__get_path(self.evacNode, node_id, self.prev_nodes)

    def __add_edge(self, from_node: str, to_node: str, weight: float):
        """
        Add a weighted edge to the graph.
        
        Args:
            from_node (str): Starting node of the edge
            to_node (str): Destination node of the edge
            weight (float): Weight/cost of the edge
        """
        if from_node not in self.nodes:
            self.nodes[from_node] = {}
        if to_node not in self.nodes:
            self.nodes[to_node] = {}
        self.nodes[from_node][to_node] = weight
        self.road_data[(from_node,to_node)] = {"cars":0,"max_speed":0,"length":weight}

    def __dijkstra(self, start: str) -> Tuple[Dict[str, float], Dict[str, str]]:
        """
        Implement Dijkstra's shortest path algorithm.
        
        Args:
            start (str): The starting node for path calculation
        
        Returns:
            Tuple of two dictionaries:
            1. Shortest distances from start node to all other nodes
            2. Previous nodes in the optimal path
        """
        # Initialize distances and previous nodes
        distances = {node: float('inf') for node in self.nodes}
        distances[start] = 0
        previous_nodes = {node: None for node in self.nodes}
        
        # Priority queue to store nodes to visit
        pq = [(0, start)]
        
        while pq:
            current_distance, current_node = heapq.heappop(pq)
            
            # If we've found a longer path, skip
            if current_distance > distances[current_node]:
                continue
            
            # Check all neighboring nodes
            if current_node in self.nodes:
                for neighbor, weight in self.nodes[current_node].items():
                    distance = current_distance + weight
                    
                    # If we've found a shorter path, update
                    if distance < distances[neighbor]:
                        distances[neighbor] = distance
                        previous_nodes[neighbor] = current_node
                        heapq.heappush(pq, (distance, neighbor))
        
        return distances, previous_nodes

    def __get_path(self, start: str, end: str, previous_nodes: Dict[str, str]) -> List[str]:
        """
        Reconstruct the shortest path between start and end nodes.
        
        Args:
            start (str): Starting node
            end (str): Destination node
            previous_nodes (Dict[str, str]): Dictionary of previous nodes from Dijkstra's algorithm
        
        Returns:
            List of nodes representing the shortest path
        """
        path = []
        current_node = end
        
        while current_node is not None:
            path.append(current_node)
            current_node = previous_nodes[current_node]
        
        # DOES NOT REVERSE PATH: THIS IS BECAUSE "start" NODE IS ACTUALLY OUR END NODE
        return list(path)

class Navigator:
    def __init__(self,evac_point):
        self.road_network = RoadGraph()
        self.road_network.setEvacNode(evac_point)
        self.road_network.calculatePaths()
        
        self.vehicle_states = {} #Key = vehicle_id, Value: {road:(a,b), length_travelled, speed, path, people, vehicle_type, distance_left, start_tick}   
        self.vehicles_to_delete = []
        self.total_in_cars = 0
        self.total_evacuated =0 
        self.total_cars = 0
        self.total_walking = 0
        self._max_walking_distance = 0
        self._terminate_distance = 0
        self._current_tick = 0
        
        self.journey_metrics = []
        self.prev_deleted = []
    
    def setMaxWalkingDistance(self,d):
        self._max_walking_distance = d
        
    def setTerminateDistance(self,d):
        self._terminate_distance = d
    
    def __getCarSpeed(self,current_speed, road):
        numCars = self.road_network.road_data[road]["cars"]
        max_speed = self.road_network.road_data[road]["max_speed"] #km per tick
        length = self.road_network.road_data[road]["length"] # Distance in km
        
        inter_car_distance = length / numCars
        
        if inter_car_distance < max_speed and numCars > 1:
            return ((1 - OVER_BREAK_SPEED_REDUCTION) * inter_car_distance) if (random.random() < _over_break_p) else inter_car_distance
        speed_kmh = tickSpeedToKmh(current_speed) #in kmh
        return min(kmhToTickSpeed(speed_kmh + ACCELERATION), max_speed)
    
    def __getNextRoad(self,vehicle_id):
        path = self.vehicle_states[vehicle_id]["path"]
        current_road = self.vehicle_states[vehicle_id]["road"]
        i = path.index(current_road[0])
        next_from_node = path[i+1]
        next_to_node = path[i+2]
        return (next_from_node,next_to_node)   
                    
    def __terminateJourney(self,vehicle_id):
        road = self.vehicle_states[vehicle_id]["road"]
        
        if self.vehicle_states[vehicle_id]["vehicle_type"] == "car":
            self.road_network.road_data[road]["cars"] -= 1
            self.total_cars -= 1
            self.total_in_cars -= self.vehicle_states[vehicle_id]["people"]
            self.total_evacuated += self.vehicle_states[vehicle_id]["people"]
            ideal_time = self.__calculateIdealTime(vehicle_id)
            # Store journey metrics for CSV
            self.journey_metrics.append({
                'car_id': vehicle_id,
                'passengers': self.vehicle_states[vehicle_id]["people"],
                'ideal_time': ideal_time,
                'actual_time': (self._current_tick - self.vehicle_states[vehicle_id]["start_tick"]) * _tick_time_mins,
                'start_tick': self.vehicle_states[vehicle_id]["start_tick"],
                'end_tick': self._current_tick
            })
            
        if self.vehicle_states[vehicle_id]["vehicle_type"] == "walking":
            self.total_walking -= 1  
            self.total_evacuated += 1
            
        self.vehicles_to_delete.append(vehicle_id)
    
    def __calculateIdealTime(self, car_id):
        """Calculate the ideal journey time if no cars were on the road"""
        path = self.vehicle_states[car_id]["path"]
        ideal_total_time = 0
        
        for i in range(len(path)-1):
            road = (path[i], path[i+1])
            road_length = self.road_network.road_data[road]["length"]
            ideal_speed = self.road_network.ideal_speeds[road]
            road_time = road_length / tickSpeedToKmh(ideal_speed)
            ideal_total_time += road_time
        
        return ideal_total_time
        
    def __getPathLength(self,path):
        i = 0
        d = 0
        while i < len(path) - 1:
            road = (path[i],path[i+1])
            d += self.road_network.road_data[road]["length"]
            i += 1
        return d
        
    def __vehicleInit(self,on_node,vehicle_type,path):
        id = 1
        while id in self.vehicle_states:
            id = random.randint(1,2**15)
        self.vehicle_states[id] = {
            "road":(on_node,path[1]),
            "length_travelled":0,
            "speed": 0 if vehicle_type == "car" else kmhToTickSpeed(5),
            "path":path,
            "people":0,
            "vehicle_type":vehicle_type,
            "distance_left": 0,
            "start_tick": self._current_tick
            }
        if vehicle_type == "car":
            self.road_network.road_data[(on_node,path[1])]["cars"] += 1
            self.total_cars += 1
        if vehicle_type == "walking":
            self.total_walking += 1
        return id
    
    def initVehicles(self, num_of_evacuees, start_node):
        # Return empty list if start node is the evacuation point
        if start_node == self.road_network.evacNode: return []
        # Return empty list if no evacuees
        if num_of_evacuees < 0 : return []
        
        vehicle_ids = []
        
        path = self.road_network.getPathFromNode(start_node)
        path_length = self.__getPathLength(path)
        vehicle_type = "car" if path_length > self._max_walking_distance else "walking"
        # print("Vehicle-type-determined:",vehicle_type)
        # Handle walking
        if vehicle_type == "walking":
            for i in range(num_of_evacuees):
                id = self.__vehicleInit(start_node,"walking",path)
                self.vehicle_states[id]["people"] = 1
                self.vehicle_states[id]["distance_left"] = path_length
                vehicle_ids.append(id)
            
            return vehicle_ids
        
        # Handle other vehicles
        # Determine capacity from vehicle type
        if vehicle_type == "car":
            vehicle_capacity = 5
            
        num_of_full_vehicles = num_of_evacuees // vehicle_capacity
        remainder = num_of_evacuees % vehicle_capacity
        
        # Init full vehicles
        for i in range(num_of_full_vehicles):
            id = self.__vehicleInit(start_node,"car",path)
            self.vehicle_states[id]["people"] = vehicle_capacity
            self.vehicle_states[id]["distance_left"] = path_length
            self.total_in_cars += vehicle_capacity
            vehicle_ids.append(id)
        # Init final vehicle with remainder    
        if remainder > 0:
            id = self.__vehicleInit(start_node,"car",path)
            self.vehicle_states[id]["people"] = remainder
            self.vehicle_states[id]["distance_left"] = path_length
            self.total_in_cars += remainder
            vehicle_ids.append(id)
        # Return all ids
        return vehicle_ids
    
    def updateVehicles(self, tick_count):
        self._current_tick = tick_count
        for vehicle_id in self.vehicle_states.keys():
            # Update vehicle speed
            # Speed = km/tick
            # print("===VEHICLE:"+str(vehicle_id)+"===")
            road = self.vehicle_states[vehicle_id]["road"]
            # Increment actual time
            # self.vehicle_states[vehicle_id]["actual_time"] += _tick_time_mins
            # print("ON-ROAD:",road)
            # print("VEHICLE-TYPE:",self.vehicle_states[vehicle_id]["vehicle_type"])
            if self.vehicle_states[vehicle_id]["vehicle_type"] == "car":
                self.vehicle_states[vehicle_id]["speed"] = self.__getCarSpeed(self.vehicle_states[vehicle_id]["speed"], road)
            # print("ROAD SPEED:", self.road_network.road_data[road]["max_speed"])
            # print("VEHICLE SPEED:",self.vehicle_states[vehicle_id]["speed"])
            # print("ROAD-LEN:",self.road_network.road_data[road]["length"])
            # Update vehicle travel distance
            # Distance travelled in tick = speed
            self.vehicle_states[vehicle_id]["length_travelled"] += self.vehicle_states[vehicle_id]["speed"]
            self.vehicle_states[vehicle_id]["distance_left"] -= self.vehicle_states[vehicle_id]["speed"]
            # Check if moved onto next road
            d_until_road_end = self.road_network.road_data[road]["length"] - self.vehicle_states[vehicle_id]["length_travelled"]
            # Check if within range to terminate
            if self.vehicle_states[vehicle_id]["distance_left"] <= self._terminate_distance:
                self.__terminateJourney(vehicle_id)
            # If moved onto next road
            while d_until_road_end <= 0 and self.vehicle_states[vehicle_id]["distance_left"] > self._terminate_distance:
                # IF BEYOND EVAC POINT, ROUTE IS FINISHED SO MARK AS FINISHED JOURNEY
                # print(road[1], self.road_network.evacNode, road[1] == self.road_network.evacNode)
                if road[1] == self.road_network.evacNode or self.vehicle_states[vehicle_id]["distance_left"] <= self._terminate_distance:
                    # print("VEHICLE",vehicle_id,"FINISHED ROUTE")
                    self.__terminateJourney(vehicle_id)
                    break
                # Move off old road
                if self.vehicle_states[vehicle_id]["vehicle_type"] == "car":
                    self.road_network.road_data[road]["cars"] -= 1
                # Calc % of tick spend moving in old road
                # print("--MOVE-ONTO-NEW_ROAD--")
                d_in_old_road = self.vehicle_states[vehicle_id]["speed"] + d_until_road_end
                t_in_old_road = d_in_old_road / self.vehicle_states[vehicle_id]["speed"]
                # print("T_OLD_ROAD:",t_in_old_road)
                # Move onto next road
                road = self.__getNextRoad(vehicle_id)
                self.vehicle_states[vehicle_id]["road"] = road
                if self.vehicle_states[vehicle_id]["vehicle_type"] == "car":
                    self.road_network.road_data[road]["cars"] += 1
                # Calc distance travelled on new road
                t_in_new_road = 1 - t_in_old_road
                # print("T-NEW_ROAD:",t_in_new_road)
                # print("NEW-ROAD:",road)
                # print("NEW-ROAD-LEN:",self.road_network.road_data[road]["length"])
                # print("NEW-ROAD-SPEED:",self.road_network.road_data[road]["max_speed"])
                if self.vehicle_states[vehicle_id]["vehicle_type"] == "car":
                    self.vehicle_states[vehicle_id]["speed"] = self.__getCarSpeed(self.vehicle_states[vehicle_id]["speed"], road)
                # print("NEW-SPEED",self.vehicle_states[vehicle_id]["speed"])
                self.vehicle_states[vehicle_id]["length_travelled"] = t_in_new_road * self.vehicle_states[vehicle_id]["speed"]
                # print("NEW-DISTANCE-TRAVELLED:",self.vehicle_states[vehicle_id]["length_travelled"])
                d_until_road_end = self.road_network.road_data[road]["length"] - self.vehicle_states[vehicle_id]["length_travelled"]
                # print("\n")
            
        # Delete all vehicles which have finished
        # print("N.o. vehicles:",len(self.vehicle_states))
        for vehicle_id in self.vehicles_to_delete:
            del self.vehicle_states[vehicle_id]
        self.prev_deleted = self.vehicles_to_delete[:]
        self.vehicles_to_delete = []
    
    def exportJourneyMetrics(self, initial_people, evacuation_prob, tick_time, interval_time, filename=None):
        """Export journey metrics to a CSV file with parameters in the filename"""
        if filename is None:
            filename = 'jm_'
            filename += f'_p{initial_people}'
            filename += f'_evp{evacuation_prob}'
            filename += f'_tick{tick_time}'
            filename += f'_max_walk_d{self._max_walking_distance}'
            filename += f'_term_d{self._terminate_distance}'
            filename += f'_interval{interval_time}'  
            filename += '.csv'
        
        # Check if file already exists, append a unique number if so
        base, ext = os.path.splitext(filename)
        i = 1
        while os.path.exists(filename):
            filename = f"{base}_{i}{ext}"
            i += 1
        
        with open(filename, 'w', newline='') as csvfile:
            fieldnames = ['car_id', 'passengers', 'ideal_time', 'actual_time', 'start_tick', 'end_tick']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            
            writer.writeheader()
            for metric in self.journey_metrics:
                writer.writerow(metric)
            
        return filename
    
    def getNoActiveCars(self):
        return self.total_cars
    
    def getNoWalking(self):
        return self.total_walking
    
    def getNoInCars(self):
        return self.total_in_cars
    
    def getNoEvacuating(self):
        return self.total_in_cars + self.total_walking
    
    def getNoEvacuated(self):
        return self.total_evacuated
    
    def getJustFinishedEvac(self):
        return self.prev_deleted
    
    def getAvgNoPeoplePerCar(self):
        if self.total_cars > 0:
            return (self.total_in_cars) / self.total_cars
        return 0
        

# # Example usage
# def main():
    
#     # with fiona.open("./data/SW_RoadNode.shp") as shapefile:
#     #     for n in shapefile:
#     #         # print(n)
    
#     evac_point = "627448CE-0C7F-4DA1-A3A5-8FD22F0FC07E"
#     nav = Navigator(evac_point)
#     #nav.initVehicles(1,"F9880B09-CE9B-4C9C-AE5E-29FA6214424E")
#     nav.initVehicles(5,"B5D7B9D7-9121-4FDA-99F7-B576A9669395")
#     while len(nav.vehicle_states) > 0:
#         nav.updateVehicles()

# if __name__ == "__main__":
#     main()