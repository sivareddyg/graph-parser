'''
Created on 17 Jun 2015

@author: siva
'''

import json
import sys

for line in sys.stdin:
    line = json.loads(line)
    entities = line['entities']
    if len(entities) < 2:
        continue
    print json.dumps(line)
