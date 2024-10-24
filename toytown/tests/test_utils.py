from toytown import utils
import unittest

osm_file = 'toytown/tests/bwaise/bwaise.osm'

class TestUtils(unittest.TestCase):

    def test_read_netlogo_file(self):
        utils.read_netlogo_file('toytown/tests/towyn/agents.txt')

    def test_convert_terrain(self):
        utils.convert_terrain('toytown/tests/bwaise/terrain.asc', 'toytown/tests/bwaise/terrain.txt')

    def test_convert_roads(self):
        utils.convert_roads(osm_file, 'toytown/tests/bwaise/roads.txt')

    def test_convert_buildings(self):
        utils.convert_buildings(osm_file,
                        'toytown/tests/bwaise/roads.txt', 'toytown/tests/bwaise/preprocessed-buildings.txt')


if __name__ == '__main__':
    unittest.main()
