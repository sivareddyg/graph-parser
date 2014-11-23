'''
Created on 22 Apr 2014

Filters sentences which do not belong to any of our target domains. 

@author: siva
'''

import sys
import json
import gzip
import os
import re

sentence_minimum_length = 3  # words
sentence_maximum_length = 21 # words
word_maximum_length = 30 # characters

def filter_sentences(facts_file, schema_file, name_to_mid_file, key_to_mid_file):
    entities_with_facts = set()
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
    
    domain_name = facts_file.split("/")[-1].split("_")[0]
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
        if len(entities) > 1 and (entities[0].startswith("m.") or entities[0] == "type.datetime") and (entities[1].startswith("m.") or entities[1] == "type.datetime"):
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
    
    key_to_mid = {}
    for line in open(key_to_mid_file):
        line = line.strip().split("\t")
        key = line[0]
        mid = line[1]
        key_to_mid[key] = mid
    
    name_to_mids = {}
    for line in open(name_to_mid_file):
        line = line.strip().split("\t")
        name = line[0].decode("utf-8", "replace")
        #if re.match("UK", name):
        #    print [name]
        mids = json.loads(line[1])
        name_to_mids[name] = mids
    
    #print name_to_mids.get("UK ", "no unicode")
    #print name_to_mids.get("UK \u00a3", "unicode")
    #  line =  {"url": "http://www.freebase.com/view/en/fukushima_i_nuclear_power_plant", "targetValue": "(list (description Japan) (description Okuma))", "utterance": "where is the fukushima daiichi nuclear plant located?"},

    date_pattern = re.compile("([0-9]{1,2}/)?([0-9]{1,2}/)?[0-9]{3,4}")
    for line in sys.stdin:
        line = line.strip()
        if not line.startswith("{"):
            continue
        line = line.strip(",\ \n")
        line = json.loads(line, 'utf-8')
        entity1_key = line['url'].split("/")[-1]
        #print json.dumps(line)
        if not key_to_mid.has_key(entity1_key):
            print json.dumps(line)
            continue
        entity1 = key_to_mid[entity1_key]
        answerEntities = re.findall("\(description \"?([^\)\"]+)\"?\)", line['targetValue'])
        
        #print "Entity1: %s" % (entity1)
        #print [line['targetValue']]
        #print answerEntities
        
        out_of_domain = False
        for entity2_name in answerEntities:
            # if re.match("UK ", entity2_name):
            #    print [entity2_name]
            entity2_candidates = []
            if name_to_mids.has_key(entity2_name):
                entity2_candidates = name_to_mids[entity2_name]
            elif date_pattern.match(entity2_name):
                entity2_candidates.append("type.datetime")
            else:
                out_of_domain = True
                break
            
            entity_pair_is_out_of_domain = True
            for entity2 in entity2_candidates:
                # print "Entity2: %s" % (entity2)
                key = (entity1, entity2)
                reverse_key = (entity2, entity1)
                if key in entities_with_facts or reverse_key in entities_with_facts:
                    entity_pair_is_out_of_domain = False
                    break
            if entity_pair_is_out_of_domain:
               out_of_domain = True
               break
        
        if not out_of_domain:
            domains = line.get('domain', [])
            if domain_name not in domains:
                domains.append(domain_name)
            line['domain'] = domains
        print json.dumps(line)
        
if __name__ == "__main__":
    # print os.getcwd()
    facts_file = sys.argv[1]
    schema_file = sys.argv[2]
    name_to_mid_file = sys.argv[3]
    key_to_mid_file = sys.argv[4]
    filter_sentences(facts_file, schema_file, name_to_mid_file, key_to_mid_file)