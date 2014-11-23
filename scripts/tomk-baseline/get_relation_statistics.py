'''
Created on 28 Feb 2014

@author: siva
'''

import sys

if __name__ == '__main__':
    tom_relation_file = sys.argv[1]
    tom_types_file = sys.argv[2]
    freebase_schema_files = sys.argv[3:]
    
    schema = {}
    types = set()
    for schema_file in freebase_schema_files:
        for line in open(schema_file):
            line = line.rstrip()
            if line == "":
                continue
            if line[0] == "\t":
                rel, arg, reltype, inv_rel = line.strip().split("\t")
                rel = rel.strip()
                inv_rel = inv_rel.strip()
                if reltype == "master":
                    schema[rel] = inv_rel
                    schema[inv_rel] = rel
                types.add(arg)
            else:
                types.add(line.split()[0])
    
    #print schema
    
    relations_covered = set() 
    relation_count = 0
    for line in open(tom_relation_file):
        # http://rdf.freebase.com/ns/tv.tv_actor.starring_roles..tv.regular_tv_appearance.series
        relation = line.split("/")[-1]
        relations = relation.split("..")
        for relation in relations:
            relation = relation.strip()
            #print relation
            if relation not in relations_covered:
                if schema.has_key(relation):
                    relation_count += 1
                    # adding both relation and its inverse
                    relations_covered.add(relation)
                    relations_covered.add(schema[relation])
    
    types_covered = set()
    type_count = 0
    for line in open(tom_types_file):
        # http://rdf.freebase.com/ns/tv.tv_actor.starring_roles..tv.regular_tv_appearance.series
        type = line.split("/")[-1].strip()
        if type not in types_covered:
            if type in types:
                type_count += 1
                types_covered.add(type)
    print "Total relations in Tom", relation_count
    print "Total types in Tom", type_count