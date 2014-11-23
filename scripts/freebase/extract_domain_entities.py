'''
Created on 19 Aug 2013

@author: Siva Reddy
'''

import re
import sys

def extract_domain_entities(domain_name):
    entities = {}
    domain_pattern = re.compile("ns:%s[\.\s]" %domain_name)
    entity_pattern = re.compile("(ns:m\.[^\s]+)") 
    for line in sys.stdin:
        if domain_pattern.search(line) != None:
            line_entities = entity_pattern.findall(line)
            for entity in line_entities:
                entity = entity.strip(".")
                if not entities.has_key(entity):
                    entities[entity] = 0
                entities[entity] += 1
    return entities

if __name__ == "__main__":
    entities = extract_domain_entities(sys.argv[1])
    items = entities.items()
    items.sort(key = lambda x : x[1], reverse = True)
    for entity, freq in items:
        print "%s\t%d" %(entity, freq)