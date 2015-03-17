'''
Created on 8 Mar 2015

@author: siva
'''

import json
import sys

file1 = open(sys.argv[1])
file2 = open(sys.argv[2])
for spanish in sys.stdin:
    english_sent = file1.readline().replace("\\", "").strip()
    spanish_sent = file2.readline().replace("\\", "").strip()
    spanish = json.loads(spanish)
    spanish['english'] = english_sent
    spanish['spanish'] = spanish_sent
    print json.dumps(spanish)
