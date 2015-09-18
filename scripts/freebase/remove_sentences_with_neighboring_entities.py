'''
Created on 26 Nov 2013

@author: Siva Reddy
'''

import sys
import json
import gzip
import re

def filter_sentences():
    for line in sys.stdin:
        # print line
        if line[0] == "#":
            print line[:-1]
            continue
        line = line.strip()
        if line == "":
            continue
        
        parts = json.loads(line)
        entities = parts['entities']
        
        if len(entities) < 2 or len(entities) > 6:
            continue
        
        entities.sort(key = lambda x : x['index'])
        
        for i in range(len(entities)):
            check = False
            for j in range(i + 1, len(entities)):
                key = (entities[i]['entity'], entities[j]['entity'])
                reverse_key = (entities[j]['entity'], entities[i]['entity'])
                if entities[j]['index'] - entities[i]['index'] == 1:
                        sys.stderr.write(line + '\n')
                        continue
                else:
                        print line
                        check = True
                        break
            if check:
                break
            
if __name__ == "__main__":
    filter_sentences()
