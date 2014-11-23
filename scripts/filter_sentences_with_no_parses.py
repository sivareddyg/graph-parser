'''
Created on 2 Oct 2013

@author: Siva Reddy
'''

import json
import sys

for line in sys.stdin:
    line = line.strip()
    parts = json.loads(line)
    if parts.has_key("synPars"):
        print line