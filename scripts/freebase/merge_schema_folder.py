'''
Created on 20 Feb 2014

@author: siva
'''

import sys
import os

schema_folder = sys.argv[1]
types = {}
schema = {}
for domain in os.listdir(schema_folder):
    with open(os.path.join(schema_folder, domain)) as fpt:
        entity = ""
        for line in fpt:
            line = line.rstrip()
            if line == '' or line[0] == '#':
                continue
            if line[0] != '\t':
                entity, entity_type  = line.split('\t')
                if entity_type.find("foreign") < 0:
                    # entity type is main/mediator belonging to the domain
                    types[entity] = entity_type
                elif entity not in types:
                    types[entity] = entity_type
            else:
                if entity not in schema:
                    schema[entity] = set()
                schema[entity].add(line)

key_priority = {"main" : 0, "mediator" : 1, "foreign" : 2, "foreign_mediator" : 3, "main_extended" : 4}

for entity, entity_type in sorted(types.items(), key = lambda x : key_priority[x[1]]):
    print "%s\t%s" %(entity, entity_type)
    if entity not in schema:
        print
        continue
    for relation in schema[entity]:
        print relation
    print
