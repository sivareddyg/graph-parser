import sys
import json
import pprint

entities = set()
for line in sys.stdin:
	sent = json.loads(line)
	for entity in sent['entities']:
		entities.add(entity['entity'])

pprint.pprint(entities)
		
