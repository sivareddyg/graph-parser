import sys
import json

sents = json.loads(sys.stdin.read())
for line in sents:
    print json.dumps(line)
