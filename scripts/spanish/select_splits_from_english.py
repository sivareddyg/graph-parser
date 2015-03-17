'''
Created on 8 Mar 2015

@author: siva
'''

import json
import sys


all = {}
for line in open(sys.argv[1]):
    line = json.loads(line)
    all[line['english']] = line

for line in open(sys.argv[2]):
    line = json.loads(line)
    print json.dumps(all[line['sentence']])
