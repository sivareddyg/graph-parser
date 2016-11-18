import sys
import json

entities = set()
for line in sys.stdin:
    sent = json.loads(line)
    for entity in sent['entities']:
        entities.add(entity['entity'])
    entities.add(sent['answerSubset'][0])

for entity in entities:
    entityDict = {"entity": entity}
    print json.dumps(entityDict)
