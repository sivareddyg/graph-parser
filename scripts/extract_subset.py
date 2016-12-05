'''
Created on 17 Apr 2015

@author: siva
'''

import json
import sys


subset = set()
for line in open(sys.argv[1]):
    if line.startswith("#") or line.strip() == "":
        continue
    line = json.loads(line)
    subset.add(line['sentence'])

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    line = json.loads(line)
    if line['sentence'] in subset:
        print json.dumps(line)
