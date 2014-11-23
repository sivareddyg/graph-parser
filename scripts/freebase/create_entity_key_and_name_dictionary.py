'''
Created on 22 Apr 2014

@author: siva

Currently use type.object.name and type.object.key. 
Full lexicon can be built using common.topic.alias

'''

import sys
import json

key_to_mid_file = open(sys.argv[1], "w")
name_to_mid_file = open(sys.argv[2], "w")

name = "ns:type.object.name"
key = "ns:type.object.key"

key_to_mid = {}
name_to_mid = {}

for line in sys.stdin:
    line = line.strip(" .\n")
    parts = line.split(" ", 2)
    if len(parts) < 3:
        continue
    entity1 = parts[0]
    relation = parts[1]
    entity2 = parts[2]
    # print parts
    
    # print relation
    if relation == key:
        # print line
        if entity2.startswith('"/en/'):
            entity2 = entity2.split('/')[-1].strip('"')
            entity1 = entity1.split(":")[-1]
            if not key_to_mid.has_key(entity2):
                key_to_mid[entity2] = set()
            key_to_mid[entity2].add(entity1)
    elif relation == name:
        # print line
        if entity2.endswith("@en"):
            entity2 = entity2.split('@en')[0].strip('"')
            entity1 = entity1.split(":")[-1]
            if not name_to_mid.has_key(entity2):
                name_to_mid[entity2] = set()
            name_to_mid[entity2].add(entity1)

for key, value in key_to_mid.items():
    value = tuple(value)
    if len(value) > 1:
        sys.stderr.write("Ambigous key, values: %s -> %s\n" % (key, value))
    else:
        key_to_mid_file.write("%s\t%s\n" %(key, value[0]))

for key, value in name_to_mid.items():
    name_to_mid_file.write("%s\t%s\n" %(key, json.dumps(tuple(value))))

key_to_mid_file.close()
name_to_mid_file.close()