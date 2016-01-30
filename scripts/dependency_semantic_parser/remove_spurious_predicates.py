'''
Created on 29 Jan 2016

@author: siva
'''

import json
import re
import sys

for line in sys.stdin:
    if line.startswith("#"):
        continue
    sent = json.loads(line)
    if "dependency_lambda" in sent:
        for parse in sent['dependency_lambda']:
            new_parse = []
            event_degree = {}
            event_predicates = []
            for element in parse:
                match = re.search(".*\(([^\)]*):e , [^\)]*:[^e\)]*\)", element)
                if match:
                    event = match.group(1)
                    event_degree.setdefault(event, [])
                    event_degree[event].append(element)
            for event, event_predicates in event_degree.items():
                if len(event_predicates) == 1:
                    parse.remove(event_predicates[0])
    print json.dumps(sent)
