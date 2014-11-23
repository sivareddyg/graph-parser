'''
Created on 21 May 2014

@author: siva
'''

import sys
import json

important_keys = set(["sentence", "targetValue", "domain", "url"])

for line in sys.stdin:
    parts = json.loads(line)
    keys = parts.keys()
    for key in keys:
        if key not in important_keys:
            del parts[key]
        if key == "sentence":
            parts["utterance"] = parts[key]
            del parts[key]
    print json.dumps(parts)