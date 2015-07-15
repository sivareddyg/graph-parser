'''
Created on 17 Apr 2015

@author: siva
'''

import json
import sys


subset = set()
for line in open(sys.argv[1]):
    line = json.loads(line)
    subset.add(line['sentence'])

for line in sys.stdin:
    line = json.loads(line)
    if line['sentence'] in subset:
        print json.dumps(line)
