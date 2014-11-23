'''
Created on 8 Jan 2014

@author: Siva Reddy
'''

import sys
import json

boundedVarCount = int(sys.argv[1])
freeVarCount = int(sys.argv[2])
freeEntityCount = int(sys.argv[3])
foreignEntityCount = int(sys.argv[4])
negationCount = int(sys.argv[5])

count = 0
for line in sys.stdin:
    line = line.strip()
    if line == "" or line[0] == "#":
        continue
    sentence = json.loads(line)
    if sentence['boundedVarCount'] <= boundedVarCount and sentence["freeVarCount"] <= freeVarCount and sentence["freeEntityCount"] <= freeEntityCount and sentence["foreignEntityCount"] <= foreignEntityCount and sentence["negationCount"] <= negationCount:
        count += 1
        print line
        
sys.stderr.write("Total number of examples: %d" %(count));