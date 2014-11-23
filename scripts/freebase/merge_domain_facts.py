'''
Created on 20 Feb 2014

@author: siva
'''

import sys
import logging
import json

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

domain_files = sys.argv[1:]

types = {}
relations = {}


for line in sys.stdin:
    line = line.rstrip()
    if line == "" or line[0] == "#":
        continue
    key, values = line.split("\t")
    #logging.info(key)
    key = json.loads(key)
    values = json.loads(values)
    if isinstance(key, list):
        key = tuple(key)
        if not relations.has_key(key):
            relations[key] = set()
        for value in values:
            relations[key].add(tuple(value))
    else:
        if not types.has_key(key):
            types[key] = set()
        types[key].update(values)
        
print "# Entity Types"
for key in types:
    print "%s\t%s" %(json.dumps(key), json.dumps(list(types[key])))

print "# Binary Relations"
for key in relations:
    print "%s\t%s" %(json.dumps(key), json.dumps(list(relations[key])))