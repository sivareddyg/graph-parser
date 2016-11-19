import sys
import json

for line in sys.stdin:
    sent = json.loads(line)
    e1 = sent['entity']
    for relation in sent['relations']:
        r1 = relation[0]
        r2 = relation[1]
        for e2 in relation[2:]:
            if e2.startswith("m.") or e2.endswith(":datetime"):
                print json.dumps([e1, r1, r2, e2])
            if e2.startswith("m."):
                print json.dumps([e2, r2, r1, e1])

