import sys
import json

for line in sys.stdin:
    if line[0] == "#" or line[0] == "\n":
        continue
    parts = json.loads(line)
    entities = parts['entities']
    boundedVarCount = parts['boundedVarCount']
    freeVarCount = parts['freeVarCount']
    foreignEntityCount = parts['foreignEntityCount']
    if len(parts['sentence'].split()) > 12 and len(entities) > 2 and boundedVarCount == 0 and freeVarCount == 0 and foreignEntityCount == 0:
        print parts['sentence']
