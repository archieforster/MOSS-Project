from .utils import create_netlogo_string
import os


class Scenario:
    def __init__(self,
                 path: str,
                 width: int,
                 height: int,
                 start_time: str,
                 end_time: str,
                 agents: list,
                 timeline: list,
                 codes: list,
                 defences: list,
                 buildings: list,
                 roads: list,
                 terrain: list,
                 streams: list = None):
        """

        :param path: folder to put all the input and output files
        :param width: width of the domain
        :param height: height of the domain
        :param start_time: start time
        :param end_time: end time
        :param agents: definitions of agent types

            [
                [
                    agent_type, origin, <list of events>
                        [origin, frequency, time, +-duration, destination, probability]
                ...]
            ... ]

            For events occurring at the same time, a choice is made using the probabilities

        :param timeline: sequence of events to take place


            [

                # either

                [
                    [[probability_distribution, time, +-duration], number],
                    [["agent", type], probability],
                    [["agent", type], probability]
                    ...
                ]

                # or

                [time, number, ["agent", type]]

                # or

                [time, [function, argument]]

                # or

                [time, [function]

                ...

            ]

        :param codes: names for building and road types
        :param defences: geometries of flood defences
        :param buildings: locations of buildings

            [
                [x, y, code, nearest_road_link]
                ...
            ]

        :param roads: geometries of roads

            [
                [road_id, origin_node_id, destination_node_id, distance, road_type, [[x, y] ...]]
                ...
            ]

        :param terrain: digital elevation model
        :param streams: gridded representation of streams
        """
        self.path = path
        self.width = width
        self.height = height
        self.start_time = start_time
        self.end_time = end_time
        self.agents = agents
        self.timeline = timeline
        self.codes = codes
        self.defences = defences
        self.buildings = buildings
        self.roads = roads
        self.terrain = terrain
        self.streams = streams

        self.create_input_files()

    def create_input_files(self):
        for sequence, filename in [

            [self.agents, 'agents.txt'],
            [self.timeline, 'timeline.txt'],
            [self.codes, 'codes.txt'],
            [self.defences, 'defences.txt'],
            [self.buildings, 'preprocessed-buildings.txt'],
            [self.roads, 'roads.txt'],
            [self.terrain, 'terrain.txt'],
            [self.streams, 'streams.txt']
        ]:
            if sequence is not None:
                self.write_data_file(filename, sequence)

    def write_data_file(self, filename: str, sequence: list):
        """

        :param filename: name of output file
        :param sequence: data to store in output file
        """
        if not os.path.exists(self.path):
            os.mkdir(self.path)
        with open(self.path + "/" + filename, "w") as f:
            f.write(create_netlogo_string(sequence))
            f.close()
