'''
Created on 29 Jan 2016

@author: siva
'''

import json
import re
import sys

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    sent = json.loads(line)
    if "dependency_lambda" in sent:
        for parse in sent['dependency_lambda']:
            new_parse = []
            event_degree = {}
            event_predicates = []
            for element in parse:
                if re.match(".*\(([^\)]*):e , ([^\)]*):e\)", element):
                    continue
                match = re.search(
                    ".*\(([^\)]*):e , ([^\)]*):.*\)", element)
                if match:
                    event = match.group(1)
                    arg = match.group(2)
                    event_degree.setdefault(event, [])
                    event_degree[event].append([element, arg])
            for event, event_predicates in event_degree.items():
                if len(event_predicates) == 1 and event == event_predicates[0][1]:
                    parse.remove(event_predicates[0][0])
    print json.dumps(sent)
