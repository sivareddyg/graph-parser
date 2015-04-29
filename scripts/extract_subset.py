'''
Created on 17 Apr 2015

@author: siva
'''

import json
import sys


all = {}
for line in sys.stdin:
    line = json.loads(line)
    all[line['sentence']] = line

for line in open(sys.argv[1]):
    line = json.loads(line)
    print json.dumps(all[line['sentence']])
