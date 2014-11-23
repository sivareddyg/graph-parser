'''
Created on 4 Jan 2014

@author: Siva Reddy
'''

import sys
from collections import Counter

schema_file = sys.argv[1]

types = Counter()
for line in sys.stdin:
    line = line.strip()
    parts = line.split()
    if len(parts) != 3:
        entityType = parts[2]
        types[entityType] += 1

for line in open(schema_file):
    print line[:-1]

for entityType in types.most_common():
    print "%s\tmain_extended" %(entityType[0].split(":", 1)[1]);
    print