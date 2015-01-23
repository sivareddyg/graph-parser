'''x y x'''
import sys
import json

for line in sys.stdin:
    if line[0] == "#" or line[0] == "\n":
        continue
    parts = json.loads(line)
    entities = parts['entities']
    if len(parts['sentence'].split()) > 12 and len(entities) > 2:
        print parts['sentence']
