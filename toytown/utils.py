import pprint
import ast
import re
import xml.etree.ElementTree as ET
try:
    import numpy as np
    import ogr
    import osr
    from scipy.spatial.distance import cdist
    from scipy.spatial import cKDTree

    src = osr.SpatialReference()
    src.ImportFromEPSG(4326)
    dest = osr.SpatialReference()
    dest.ImportFromEPSG(21096)
    default_transform = osr.CoordinateTransformation(src, dest)

except ImportError:
    pass

def create_netlogo_string(sequence: list, **kwargs):
    """

    :param sequence:
    :return:
    """

    return pprint.pformat(sequence, **kwargs).replace(',', ' ').replace('"', '\\').replace("'", '"')[1:-1]


def read_netlogo_file(path):
    with open(path) as f:
        lines = f.readlines()

    lines = [line.strip() for line in lines if not line.startswith(';')]

    string = ' '.join(lines)
    string = re.sub(r'([\d\.]+|"[\w\/\s:&]+"|\]|[^\W\s]+)', r'\1,', string)

    return ast.literal_eval('['+string+']')


def convert_terrain(in_path, out_path):

    with open(in_path) as f:
        lines = [line.strip().split() for line in f.readlines()]

    lines.insert(6, [])

    string = create_netlogo_string(lines)

    string = re.sub(r'"([\d\.-]+)"', r'\1', string)

    with open(out_path, 'w') as f:
        f.write(string)


def convert_roads(in_path, out_path, **kwargs):
    with open(in_path) as f:
        tree = ET.fromstring(f.read())

    highways = tree.findall("way//*[@k='highway']..")
    roads = []
    for highway in highways:
        highway_id = highway.attrib['id']
        nodes = highway.findall('nd')
        highway_type = highway.find("*[@k='highway']").attrib['v']
        xy = []
        ids = []
        for node in nodes:
            node_id = node.attrib['ref']
            element = tree.find("node[@id='{}']".format(node_id))
            attrib = element.attrib
            xy.append(reproject(float(attrib['lat']), float(attrib['lon'])))

            ids.append(node_id)

        xy = np.array(xy)

        distance = cdist(xy[:-1], xy[1:]).sum().round(0).astype(int)

        # x and y coordinates are multiplied by 1000 because this is expected by netlogo code
        roads.append([highway_id, ids[0], ids[-1], distance, highway_type, (xy*1000).tolist()])


    with open(out_path, 'w') as f:
        f.write(create_netlogo_string(roads))


def convert_buildings(in_path, roads_path, out_path, **kwargs):
    with open(in_path) as f:
        tree = ET.fromstring(f.read())

    roads = read_netlogo_file(roads_path)
    x = []
    y = []
    node_ids = []

    for road in roads:
        _, origin, destination, _, _, coords = road
        origin_x, origin_y = coords[0]
        destination_x, destination_y = coords[-1]
        node_ids.append(origin)
        x.append(origin_x)
        y.append(origin_y)

        node_ids.append(destination)
        x.append(destination_x)
        y.append(destination_y)

    # convert x and y points back into actual coordinates by dividing by 1000
    nodes_kd_tree = cKDTree(np.transpose([x, y])/1000)

    building_ways = tree.findall("way//*[@k='building']..")
    xy = []
    building_types = []
    for building in building_ways:
        nodes = building.findall('nd')
        building_type = building.find("*[@k='building']").attrib['v']
        building_types.append(buildings_types_lookup[building_type])
        lats = []
        lons = []
        for node in nodes:
            node_id = node.attrib['ref']
            element = tree.find("node[@id='{}']".format(node_id))
            attrib = element.attrib
            lats.append(float(attrib['lat']))
            lons.append(float(attrib['lon']))

        lat, lon = np.mean(np.transpose([lats, lons]), axis=0)

        xy.append(reproject(lat, lon))

    amenity_nodes = tree.findall("node//*[@k='amenity']..")
    for amenity in amenity_nodes:
        building_type = amenity.find("*[@k='amenity']").attrib['v']
        xy.append(reproject(amenity.attrib['lat'], amenity.attrib['lon']))
        building_types.append(buildings_types_lookup[building_type])

    xy = np.array(xy)
    _, index = nodes_kd_tree.query(xy)
    x, y = xy.T
    buildings = [list(row) for row in zip(x, y, building_types, np.array(node_ids)[index])]

    with open(out_path, 'w') as f:
        f.write(create_netlogo_string(buildings))


def reproject(lat: float, lon: float, transform=default_transform):

    point = ogr.CreateGeometryFromWkt("POINT ({} {})".format(lat, lon))
    point.Transform(transform)

    return point.GetX(), point.GetY()


buildings_types_lookup = {
    "yes": 0,
    "commercial": -3,
    "hut": 0,
    "industrial": 830,
    "residential": 0,
    "bank": 320,
    "fuel": 222,
    "bus_station": 940,
    "clinic": 660,
    "mobile_money_agent": -3,
    "recycling": -3,
    "school": 610,
    "house": 0,
    "construction": -3,
    "detached": 0,
    "hotel": 511,
    "none": 0,
    "doctors": 660,
    "driving_school": -4,
    "atm": -4,
    "roof": -4,
    "kindergarten": 610,
    "sacco": 320,
}
