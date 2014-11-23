'''
Created on 30 Aug 2013

Filters sentences which contain at least one pair of entities in relation.

@author: Siva Reddy

'''

import sys
import json
import gzip
import os

sentence_minimum_length = 3  # words
sentence_maximum_length = 21 # words
word_maximum_length = 30 # characters

def filter_sentences(facts_file, schema_file):
    # relations that have at least one main type
    types_all = {}
    relations_all = {}
    f = open(schema_file)
    line = f.readline()
    while line != "":
        line = line.rstrip();
        if line == "":
            line = f.readline()
            continue
        if line[0] != "\t":
            parts = line.strip().split('\t');
            parent = parts[0]
            parent_type = parts[1]
            types_all[parent] = parent_type
            line = f.readline()
            while line != "" and line.strip() != "":
                if line[0] == '\t':
                    parts = line.strip().split('\t')
                    rel = parts[0]
                    child = parts[1]
                    rel_type = parts[2]
                    rel_inv = parts[3]
                    # print line
                    relations_all[rel] = (parent, child)
                    relations_all[rel_inv] = (child, parent)
                line = f.readline()
    f.close()
    # print relations_all
    # print types_all
    # exit()
    
    entities_with_facts = set()
    for line in gzip.open(facts_file):
        if line[0] == "#":
            continue
        line = line.strip()
        if line == "":
            continue
        if line[0] != "[":
            continue
        
        # sentences are extracted using binary relations formed by two entities from the domain
        entities = json.loads(line.split('\t')[0])
        relations = json.loads(line.split('\t')[1])
        if len(entities) > 1 and entities[0].startswith("m.") and entities[1].startswith("m."):
            for relation in relations:
                check = False;
                for relation_part in relation:
                    if relations_all.has_key(relation_part) and ((types_all.has_key(relations_all[relation_part][0]) and types_all[relations_all[relation_part][0]] == "main") or (types_all.has_key(relations_all[relation_part][1]) and types_all[relations_all[relation_part][1]] == "main")):
                        entity1 = "/" + entities[0].replace(".", "/")
                        entity2 = "/" + entities[1].replace(".", "/")
                        # print (entity1, entity2)
                        entities_with_facts.add((entity1, entity2))
                        check = True
                        break;
                if check:
                    break
    
    sys.stderr.write("No. of facts loaded: %d\n" % (len(entities_with_facts)))
    
    # line = '''[" Bill Gates became the richest man on the planet, and Microsoft came to dominate the software industry worldwide.", [["/m/017nt", 0.99784300000000004, 1, 11], ["/m/04sv4", 0.99997199999999997, 54, 63]]]'''
    # line = '''[" Brad Pitt and Angelina Jolie hit the red carpet at the 2009 Golden Globe Awards held at .", [["/m/0c6qh", 0.99999199999999999, 1, 10], ["/m/0f4vbz", 0.99999300000000002, 15, 29]]]'''
    # while line != "":
    for line in sys.stdin:
        line = line.rstrip()
        parts = json.loads(line)
        
        sent = parts[0].split()
        if len(sent) < sentence_minimum_length:
            continue
        if len(sent) > sentence_maximum_length:
            continue
        word_is_long = False
        for word in sent:
            if len(word) > word_maximum_length:
                word_is_long = True
                break
        if word_is_long:
            continue
        
        if len(parts[1]) < 2:
            continue
        
        entities = parts[1]
        
        if len(entities) < 2 or len(entities) > 4:
            continue
        
        # print parts
        check = False
        for word_index in range(0, len(entities)):
            for j in range(word_index + 1, len(entities)):
                key = (entities[word_index][0], entities[j][0])
                reverse_key = (entities[j][0], entities[word_index][0])
                if key in entities_with_facts or reverse_key in entities_with_facts:
                    print line
                    check = True
                    break
            if check:
                break
    
if __name__ == "__main__":
    # print os.getcwd()
    facts_file = sys.argv[1]
    schema_file = sys.argv[2]
    filter_sentences(facts_file, schema_file)
