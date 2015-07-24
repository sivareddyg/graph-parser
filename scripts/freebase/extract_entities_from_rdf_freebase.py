'''
Created on 21 Jul 2015

@author: siva
'''

import re
import sys

entities = set()
for line in sys.stdin:
    line = line.strip()
    parts = line.split("\t", 3)
    if len(parts) != 3:
        continue
    # print parts
    matcher = re.search("<http://rdf.freebase.com/ns/(m\..*)>", parts[0])
    if matcher:
        entities.add(matcher.group(1))
    matcher = re.search("<http://rdf.freebase.com/ns/(m\..*)>", parts[2])
    if matcher:
        entities.add(matcher.group(1))

for entity in entities:
    print entity
