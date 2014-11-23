'''
Created on 26 Nov 2013

@author: Siva Reddy
'''

import sys
import json
import gzip
import re

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
                        entity1 = entities[0]
                        entity2 = entities[1]
                        # print (entity1, entity2)
                        entities_with_facts.add((entity1, entity2))
                        check = True
                        break;
                if check:
                    break
    
    sys.stderr.write("No. of facts loaded: %d\n" % (len(entities_with_facts)))

    for line in sys.stdin:
        # print line
        if line[0] == "#":
            print line[:-1]
            continue
        line = line.strip()
        if line == "":
            continue
        
        parts = json.loads(line)
        entities = parts['entities']
        
        if len(entities) < 2 or len(entities) > 4:
            continue
        
        entities.sort(key = lambda x : x['index'])
        
        for i in range(len(entities)):
            check = False
            for j in range(i + 1, len(entities)):
                key = (entities[i]['entity'], entities[j]['entity'])
                reverse_key = (entities[j]['entity'], entities[i]['entity'])
                if key in entities_with_facts or reverse_key in entities_with_facts:
                    if entities[j]['index'] - entities[i]['index'] == 2:
                        # if the entities are separtated by punctuation
                        if (re.search("[a-zA-Z0-9]", parts['words'][entities[i]['index'] + 1]['word']) == None):
                            continue
                        else:
                            print line
                            check = True
                            break
                    elif entities[j]['index'] - entities[i]['index'] == 1:
                        continue
                    else:
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