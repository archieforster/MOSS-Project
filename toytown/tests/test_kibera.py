#!/usr/bin/env python
# -*- coding: utf-8 -*-

from toytown import run, scenario
import os
from toytown.utils import read_netlogo_file

agents = read_netlogo_file('toytown/tests/kibera/agents.txt')

defences_to_breach = ["a"]

defences = read_netlogo_file('toytown/tests/kibera/defences.txt')

roads = read_netlogo_file('toytown/tests/kibera/roads.txt')

codes = read_netlogo_file('toytown/tests/kibera/codes.txt')

terrain = read_netlogo_file('toytown/tests/kibera/terrain.txt')

buildings = read_netlogo_file('toytown/tests/kibera/preprocessed-buildings.txt')

streams = read_netlogo_file('toytown/tests/kibera/streams.txt')


warning_times = ["07:50:00", "07:55:00", "08:00:00"]

sea_levels = [4]

if not os.path.exists('toytown/tests/outputs'):
    os.mkdir('toytown/tests/outputs')

model_path = 'toytown/tests/outputs/kibera'
inputs_path = 'toytown/tests/kibera'

scenarios = []


for defence in defences_to_breach:
    for warning_time in warning_times:
        for sea_level in sea_levels:
            scenarios.append(
                scenario.Scenario(
                    path='toytown/tests/outputs/kibera-defence-{}-evacuation-{}-sea-{}'.format(defence, warning_time.replace(':', ''), sea_level),
                    width=297,
                    height=104,
                    start_time='7:45',
                    end_time='8:20',
                    agents=agents,
                    roads=roads,
                    codes=codes,
                    defences=defences,
                    buildings=buildings,
                    terrain=terrain,
                    streams=streams,
                    timeline=
                    [
                        [
                            [
                                ["normal", "08:00", "15m"],
                                500,
                                ["agent", "transit eastbound"],
                                0.8,
                                ["agent", "transit westbound"],
                                0.2
                            ],
                            [
                                "0s",
                                10, # number of agents
                                ["agent", "kids"]
                            ],
                            [
                                "07:54",
                                ["sealevel", sea_level]
                            ],
                            [
                                "07:55",
                                ["breach", defence]
                            ],
                            [
                                warning_time,
                                ["evacuate"]
                            ]
                        ]
                    ]))

r = run.Run(scenarios=scenarios)

r.setup_and_run()
