'''
Created on 27 Apr 2015

@author: siva
'''

import json
import sys

for line in sys.stdin:
    line = json.loads(line)
    line['sentence'] = line['utterance']
    del line['utterance']
    print json.dumps(line)
