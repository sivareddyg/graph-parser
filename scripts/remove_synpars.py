import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    if 'synPars' in sent:
        del sent['synPars']
    print json.dumps(sent)
