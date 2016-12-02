import json
import sys

for line in sys.stdin:
	parts = line.strip().split("\t", 3)
	parts[2] = json.dumps([json.loads(parts[2])[0]]) if len(json.loads(parts[2])) > 0 else json.dumps([])
	print "\t".join(parts)
