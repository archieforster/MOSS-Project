#!/usr/bin/env python
# -*- coding: utf-8 -*-


from toytown import run
import os
from toytown.scenario import Scenario
from toytown.utils import read_netlogo_file


agents = [
    ["kids & work", "home 1", [
        ["home 1", "daily 7:46 0m", "school", 1],
        ["school", "5m 1m", "work", 0.9],
        ["school", "5m 1m", "some shop", 0.1],
        ["some shop", "2h 1h", "work", 1],
        ["work", "daily 17h 15m", "home 1", 0.75],
        ["work", "daily 17h 15m", "supermarket", 0.15],
        ["work", "daily 17h 15m", "home 2", 0.1],
        ["supermarket", "45m 10m", "home 1", 1],
        ["home 2", "daily 20h 2h", "some recreation", 0.1],
        ["some recreation", "3h 1h", "home 1", 1],

        ["evacuate", "5m 1m", "some evacuation point", 0.9],
        ["evacuate", "0s 0s", "resume", 0.1],
        ["some evacuation point", "daily 4h 1h", "home 1", 1]
    ]],
    ["test2", "home", [
        ["home", "0m 0m", "school", 1],
        ["school", "0m 0m", "home", 1],

        ["evacuate", "0m 0m", "some evacuation point", 0.9],
        ["evacuate", "0s 0s", "resume", 0.1],
        ["some evacuation point", "0m 0m", "home", 1],
    ]],
    ["transit eastbound", "A55 west", [
        ["A55 west", "0s 0s", "A55 east", 1],
        ["A55 east", "0s 0s", "exit", 1],
    ]],
    ["transit westbound", "A55 east", [
        ["A55 east", "0s 0s", "A55 west", 1],
        ["A55 west", "0s 0s", "exit", 1],
    ]],
    ["test", "towyn test", [
        ["towyn test", "0s 0s", "rhyl test", 1],
        ["rhyl test", "0s 0s", "towyn test", 1],

        ["evacuate", "0s 0s", "some evacuation point", 1],
        ["some evacuation point", "5m 1m", "towyn test", 1],
    ]]]

warning_times = ["07:50:00", "07:51:00", "07:52:00"]

sea_levels = [4, 5, 6]

codes = read_netlogo_file('toytown/tests/towyn/codes.txt')

defences = read_netlogo_file('toytown/tests/towyn/defences.txt')

defences_to_breach = ["a"]

buildings = read_netlogo_file('toytown/tests/towyn/preprocessed-buildings.txt')

roads = read_netlogo_file('toytown/tests/towyn/roads.txt')

terrain = read_netlogo_file('toytown/tests/towyn/terrain.txt')

if not os.path.exists('toytown/tests/outputs'):
    os.mkdir('toytown/tests/outputs')

inputs_path = 'toytown/tests/towyn'
model_path = 'toytown/tests/outputs/towyn'

scenarios = []

for defence in defences_to_breach:
    for warning_time in warning_times:
        for sea_level in sea_levels:
            timeline = [
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
                        10,  # number of agents
                        ["agent", "kids & work"]
                    ],
                    [
                        "07:54",
                        ["sealevel", sea_level]
                    ],
                    [
                        "07:55",
                        ["breach", "a"]
                    ],
                    [
                        warning_time,
                        ["evacuate"]
                    ]
                ]
            ]

            scenarios.append(
                Scenario(path='toytown/tests/outputs/towyn-defence-{}-evacuation-{}-sea-{}'.format(defence,
                                                                                           warning_time.replace(':', ''),
                                                                                           sea_level),
                         width=249,
                         height=179,
                         start_time='7:45',
                         end_time='8:20',
                         agents=agents,
                         timeline=timeline,
                         codes=codes,
                         defences=defences,
                         buildings=buildings,
                         roads=roads,
                         terrain=terrain))

r = run.Run(scenarios=scenarios)

r.setup_and_run()
