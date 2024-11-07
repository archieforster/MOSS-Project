import heapq
import fiona
import random
from typing import Dict, List, Tuple

TICK_TIME_MINS = 5

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
        with fiona.open("./data/SW_RoadLink.shp") as shapefile:
            for road in shapefile:
                # Add graph edges
                start = road.properties["startNode"]
                end = road.properties["endNode"]
                length = road.properties["length"]
                self.__add_edge(start,end,length)
                self.__add_edge(end,start,length)
                # Add speed limit data
                roadType = road.properties["formOfWay"]
                if roadType == "Single Carriageway":
                    max_speed = self.__kmhToTickSpeed(96) # Single Carriageway => 60mph, 96 km/h
                elif roadType == "Dual Carriageway":
                    max_speed = self.__kmhToTickSpeed(112) # Dual Carriageway => 70mph, 112 km/h
                else:
                    max_speed = self.__kmhToTickSpeed(32) # Default 20mph speed limit as 32km/h
                self.road_data[(start,end)]["max_speed"] = max_speed
                
            
    
    def setEvacNode(self, node_id):
        self.evacNode = node_id

    def calculatePaths(self):
        distances, prev_nodes = self.__dijkstra(self.evacNode)
        self.prev_nodes = prev_nodes
    
    def getPathFromNode(self, node_id):
        return self.__get_path(self.evacNode, node_id, self.prev_nodes)
    
    def __kmhToTickSpeed(self,kmh_speed):
        return kmh_speed / (60 / TICK_TIME_MINS) # Assume tick = 5 mins

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
        self.road_data[(from_node,to_node)] = {"cars":0,"max_speed":30,"length":weight}

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
        
        self.car_states = {} #Key = car_id, Value: {road:(a,b), length_travelled, speed, path}   
        self.cars_to_delete = set()  
    
    def __getCarSpeed(self,road):
        numCars = self.road_network.road_data[road]["cars"]
        max_speed = self.road_network.road_data[road]["max_speed"]
        length = self.road_network.road_data[road]["length"]
        
        if numCars > length:
            return max_speed / (numCars / length)
        return max_speed
    
    def __getNextRoad(self,car_id):
        path = self.car_states[car_id]["path"]
        current_road = self.car_states[car_id]["road"]
        i = path.index(current_road[0])
        next_from_node = path[i+1]
        next_to_node = path[i+2]
        return (next_from_node,next_to_node)   
                    
    def __terminateJourney(self,car_id):
        road = self.car_states[car_id]["road"]
        self.road_network.road_data[road]["cars"] -= 1
        self.cars_to_delete.add(car_id)
        
    def carInit(self,on_node):
        id = 1
        while id in self.car_states:
            id = random.randint(1,2**15)
        path = self.road_network.getPathFromNode(on_node)
        self.car_states[id] = {
            "road":(on_node,path[1]),
            "length_travelled":0,
            "speed":0,
            "path":path
            }
        self.road_network.road_data[(on_node,path[1])]["cars"] += 1
        return id
        
    def updateCars(self):
        for car_id in self.car_states.keys():
            # Update car speed
            # Speed = km/tick
            print("===CAR:"+str(car_id)+"===")
            road = self.car_states[car_id]["road"]
            print("ON-ROAD:",road)
            self.car_states[car_id]["speed"] = self.__getCarSpeed(road)
            print("SPEED:",self.car_states[car_id]["speed"])
            print("ROAD-LEN:",self.road_network.road_data[road]["length"])
            # Update car travel distance
            # Distance travelled in tick = speed
            self.car_states[car_id]["length_travelled"] += self.car_states[car_id]["speed"]
            # Check if moved onto next road
            d_until_road_end = self.road_network.road_data[road]["length"] - self.car_states[car_id]["length_travelled"]
            # If moved onto next road
            while d_until_road_end <= 0:
                # IF BEYOND EVAC POINT, ROUTE IS FINISHED SO MARK AS FINISHED JOURNEY
                print(road[1], self.road_network.evacNode, road[1] == self.road_network.evacNode)
                if road[1] == self.road_network.evacNode:
                    print("CAR",car_id,"FINISHED ROUTE")
                    self.__terminateJourney(car_id)
                    break
                # Move off old road
                self.road_network.road_data[road]["cars"] -= 1
                # Calc % of tick spend moving in old road
                print("--MOVE-ONTO-NEW_ROAD--")
                d_in_old_road = self.car_states[car_id]["speed"] + d_until_road_end
                t_in_old_road = d_in_old_road / self.car_states[car_id]["speed"]
                print("T_OLD_ROAD:",t_in_old_road)
                # Move onto next road
                road = self.__getNextRoad(car_id)
                self.car_states[car_id]["road"] = road
                self.road_network.road_data[road]["cars"] += 1
                # Calc distance travelled on new road
                t_in_new_road = 1 - t_in_old_road
                print("T-NEW_ROAD:",t_in_new_road)
                print("NEW-ROAD:",road)
                self.car_states[car_id]["speed"] = self.__getCarSpeed(road)
                print("NEW-SPEED",self.car_states[car_id]["speed"])
                self.car_states[car_id]["length_travelled"] = t_in_new_road * self.car_states[car_id]["speed"]
                print("NEW-ROAD-LEN:",self.road_network.road_data[road]["length"])
                print("NEW-DISTANCE-TRAVELLED:",self.car_states[car_id]["length_travelled"])
                d_until_road_end = self.road_network.road_data[road]["length"] - self.car_states[car_id]["length_travelled"]
            print("\n")
            
        # Delete all cars which have finished
        for car_id in self.cars_to_delete:
            del self.car_states[car_id]
        self.cars_to_delete = set()       

# Example usage
def main():
    evac_point = "627448CE-0C7F-4DA1-A3A5-8FD22F0FC07E"
    nav = Navigator(evac_point)
    nav.carInit("F4FEF811-77A9-48F5-8B2D-7B64C2E0D317")
    while len(nav.car_states) > 0:
        nav.updateCars()
    

if __name__ == "__main__":
    main()