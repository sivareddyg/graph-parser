'''
Created on 5 Nov 2015

@author: siva
'''

import copy
import json
import sys


for line in sys.stdin:
    sentence = json.loads(line)
    keys = ['sentence', 'goldMid', 'goldRelations',
            'url', 'targetValue', 'index']
    sent_keys = copy.copy(sentence.keys())
    for key in sent_keys:
        if key not in keys:
            del sentence[key]
    print json.dumps(sentence)
