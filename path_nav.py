import heapq
import fiona
from typing import Dict, List, Tuple

class RoadGraph:
    def __init__(self):
        """
        Initialize an empty graph using an adjacency list representation.
        The graph is stored as a dictionary where:
        - Keys are nodes
        - Values are dictionaries of neighboring nodes and their edge weights
        """
        self.nodes = {}
    
        with fiona.open("./data/TG_RoadLink.shp") as shapefile:
            for road in shapefile:
                start = road.properties["startNode"]
                end = road.properties["endNode"]
                length = road.properties["length"]
                self.__add_edge(start,end,length)
                self.__add_edge(end,start,length)
    
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

# Example usage
def main():
    # Get Dataset
    navigator = RoadGraph()
    evac_node = '081F9FA5-31D7-4E17-87E2-6197C03B7595'
    navigator.setEvacNode(evac_node)
    navigator.calculatePaths()
    
    for i in navigator.nodes:
        print(i, navigator.nodes[i])
    print(navigator.getPathFromNode('42A5574A-8B9B-4E0D-9402-C1684ABA33FF'))
    
    
    # # Create a graph
    # graph = Graph()
    
    # # Add edges
    # graph.add_edge('A', 'B', 4)
    # graph.add_edge('A', 'C', 2)
    # graph.add_edge('B', 'D', 3)
    # graph.add_edge('C', 'B', 1)
    # graph.add_edge('C', 'D', 5)
    # graph.add_edge('D', 'E', 2)
    
    # # Run Dijkstra's algorithm from node 'A'
    # start_node = 'A'
    # distances, previous_nodes = graph.dijkstra(start_node)
    # print(previous_nodes)
    
    # # Example: Find path from 'A' to 'E'
    # path = graph.get_path(start_node, 'E', previous_nodes)
    # print("\nShortest Path from A to E:", ' -> '.join(path))
    # print("Total Distance:", distances['E'])

if __name__ == "__main__":
    main()