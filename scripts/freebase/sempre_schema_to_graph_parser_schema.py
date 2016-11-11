'''
Created on 11 Nov 2016

Sempre schema can be downloaded from http://nlp.stanford.edu/software/sempre/dependencies-2.0/u/nlp/data/semparse/scr/freebase/state/execs/93.exec/schema2.ttl/ 

@author: siva
'''

import sys

mediators = {}
types_to_relations = {}
relations_subject_type = {}
relations_to_expected_types = {}
relation_is_master = {}
relation_to_inverse = {}
relations = set([])

for line in sys.stdin:
    parts = line.rstrip().strip(".").replace("fb:", "").split("\t", 3)
    if parts[1] == "type.property.schema":
        if parts[2] not in types_to_relations:
            types_to_relations[parts[2]] = []
        types_to_relations[parts[2]].append(parts[0])
        relations_subject_type[parts[0]] = parts[2]
        relations.add(parts[0]) 
    elif parts[1] == "type.property.expected_type":
        relations_to_expected_types[parts[0]] = parts[2]
        relations.add(parts[0])
    elif parts[1] == "type.property.reverse_property":
        relation_is_master[parts[0]] = True
        relation_is_master[parts[2]] = False
        relation_to_inverse[parts[0]] = parts[2]
        relation_to_inverse[parts[2]] = parts[0]
        relations.add(parts[0])
        relations.add(parts[1])
    elif parts[1] == "freebase.type_hints.mediator":
        mediators[parts[0]] = True

for relation in relations:
    # relations with mediators as expected type should always have an inverse
    if relation not in relation_to_inverse and relation in relations_to_expected_types and relations_to_expected_types[relation] in mediators:
        mediator = relations_to_expected_types[relation]
        inverse_relation = relation + ".inverse"
        if mediator not in types_to_relations:
            types_to_relations[mediator] = []
        types_to_relations[mediator].append(inverse_relation)
        relations_to_expected_types[inverse_relation] = relations_subject_type[relation]
        relations_subject_type[inverse_relation] = mediator
        relation_to_inverse[relation] = inverse_relation
        relation_to_inverse[inverse_relation] = relation
        relation_is_master[relation] = True
        relation_is_master[inverse_relation] = False

for fbtype in types_to_relations:
    if fbtype in mediators:
        continue
    print "%s\tmain" % (fbtype)
    for relation in types_to_relations[fbtype]:
        if relation not in relations_to_expected_types:
            continue
        expected_type = relations_to_expected_types[relation]
        master_or_reverse = "master" if relation_is_master.get(relation, True) else "reverse"
        inverse_relation = relation_to_inverse.get(relation, "none")
        print "\t%s\t%s\t%s\t%s" % (relation, expected_type, master_or_reverse, inverse_relation)
    print
    
for fbtype in types_to_relations:
    if fbtype not in mediators:
        continue
    print "%s\tmediator" % (fbtype)
    for relation in types_to_relations[fbtype]:
        # print "Hello", relation
        if relation not in relations_to_expected_types:
            continue
        expected_type = relations_to_expected_types[relation]
        master_or_reverse = "master" if relation_is_master.get(relation, True) else "reverse"
        inverse_relation = relation_to_inverse.get(relation, "none")
        print "\t%s\t%s\t%s\t%s" % (relation, expected_type, master_or_reverse, inverse_relation)
    print
