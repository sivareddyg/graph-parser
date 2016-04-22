'''
Created on 19 Apr 2016

@author: siva
'''
from collections import Counter
import json
import sys


total = 0.0
matchesFound = 0
patternCounter = Counter()
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
    matches = set()
    for matchedEntity in sent['matchedEntities']:
        if "rankedEntities" not in matchedEntity:
            continue
        for rankedEntity in matchedEntity["rankedEntities"]:
            mid = rankedEntity["entity"]
            if mid == goldMid:
                matchFound = True
                matches.add(matchedEntity["pattern"])

    for match in matches:
        patternCounter[match] += 1.0 / len(matches)
    if matchFound:
        matchesFound += 1

print "%d %d %.2f" % (matchesFound, total, matchesFound / total * 100)

totalFreqCount = 0.0
for _, freq in patternCounter.items():
    totalFreqCount += freq

countSoFar = 0.0
for pattern, freq in patternCounter.most_common():
    countSoFar += freq
    print pattern, "\t", freq, "\t", countSoFar / totalFreqCount * 100
