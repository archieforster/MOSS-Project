#!/usr/bin/env python
# -*- coding: utf-8 -*-

import subprocess
import os

from xml.etree.ElementTree import Element, SubElement

from xml.etree import ElementTree
from xml.dom import minidom
from xml.sax.saxutils import unescape

netlogo = os.environ['NETLOGO']

model = 'model.nlogo'


class Run:
    def __init__(self, scenarios, setup_path='setup.xml'):
        self.scenarios = scenarios
        self.setup_path = setup_path

    def write_setup_file(self, seed=1):

        experiments = Element('experiments')

        experiment = SubElement(experiments, 'experiment',
                                dict(name='toytown', repetitions="1", runMetricsEveryStep='false'))

        setup = SubElement(experiment, 'setup')
        setup.text = 'setup pathshow-setup'

        go = SubElement(experiment, 'go')
        go.text = 'model-step'

        final = SubElement(experiment, 'final')
        final.text = 'write-final-report'

        exit_condition = SubElement(experiment, 'exitCondition')
        exit_condition.text = 'ticks &gt; end-time'

        metric = SubElement(experiment, 'metric')
        metric.text = '(list end-time agents-drowned agents-diverted agents-isolated)'

        def quote(string):
            return "&quot;{}&quot;".format(string)

        for variable, value in [
            ('start-time', quote(self.scenarios[0].start_time)),
            ('heuristic-factor', '1.25'),
            ('log-interval', quote('2m')),
            ('end-time-str', quote(self.scenarios[0].end_time)),
            ('random-seed', str(seed)),
            ('world-width', str(self.scenarios[0].width)),
            ('world-height', str(self.scenarios[0].height))

        ]:
            element = SubElement(experiment, 'enumeratedValueSet', {'variable': variable})
            SubElement(element, 'value', {'value': value})

        element = SubElement(experiment, 'enumeratedValueSet', {'variable': 'Scenario'})
        for scenario in self.scenarios:

            SubElement(element, 'value', {'value': quote(str(scenario.path))})

        header = r'<?xml version="1.0" encoding="utf-8"?><!DOCTYPE experiments SYSTEM "behaviorspace.dtd">'
        dom = minidom.parseString(header + ElementTree.tostring(experiments).decode('utf-8'))
        with open(self.setup_path, 'w') as f:
            f.write(unescape(dom.toprettyxml()))

    def run(self):
        args = ['java',
                '-Xmx1024M',
                '-cp',
                netlogo,
                'org.nlogo.headless.HeadlessWorkspace',
                '--table',
                'table-output.csv',
                '--model',
                model,
                '--setup-file',
                self.setup_path,
                '--experiment',
                'toytown']
        p = subprocess.Popen(args)
        p.wait()
        print("done with %s" % str(p.returncode))

    def setup_and_run(self, **kwargs):

        self.write_setup_file(**kwargs)
        self.run()
