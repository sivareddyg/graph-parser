'''
Created on 18 Apr 2016

@author: siva
'''

import json
import sys


total = 0.0
matchesFound = 0
for line in sys.stdin:
    if line.startswith("#"):
        continue
    if line.strip() == "":
        continue

    total += 1.0
    sent = json.loads(line)
    goldMid = sent['goldMid']

    matchFound = False
    if "matchedEntities" not in sent:
        continue
    for matchedEntity in sent['matchedEntities']:
        if "rankedEntities" not in matchedEntity:
            continue
        for rankedEntity in matchedEntity["rankedEntities"]:
            mid = rankedEntity["entity"]
            if mid == goldMid:
                matchFound = True
                break
        if matchFound:
            break
    if matchFound:
        matchesFound += 1

print "%d %d %.2f" % (matchesFound, total, matchesFound / total)
