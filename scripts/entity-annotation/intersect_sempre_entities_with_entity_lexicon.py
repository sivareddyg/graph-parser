'''
Created on 27 Apr 2015

@author: siva
'''

import os
import re
import sys

sempre_entities = set()

# format: m.03245
for line in os.popen("zcat %s" % (sys.argv[1])):
    line = line.strip()
    sempre_entities.add(line)

# format: m.0100m0c\tCrusade of Fanaticism
for line in sys.stdin:
    parts = line.split("\t", 1)
    if parts[0] in sempre_entities:
        sys.stdout.write(line)
