import sys
import json

fields = sys.argv[1].split(",")
for line in sys.stdin:
    line = line.strip()
    if line.startswith("#") or line=="":
        continue
    sent = json.loads(line)
    for field in fields:
        if field in sent:
            del sent[field]
    print json.dumps(sent)
