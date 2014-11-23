'''
Created on 26 Nov 2013

@author: Siva Reddy
'''

import sys
import json

for line in sys.stdin:
    line = line.strip()
    sent = json.loads(line)
    