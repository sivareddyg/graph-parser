'''
Created on 30 Aug 2013

Filters sentences which contain at least one pair of entities in relation.

@author: Siva Reddy

'''

import sys
import json
import gzip

def filter_sentences(facts_file):
    entities_with_facts = set()
    for line in gzip.open(facts_file):
        if line[0] == "#":
            continue
        line = line.strip()
        if line == "":
            continue
        entities = json.loads(line.split('\t')[0])
        entities[0] = "/" + entities[0].replace(".", "/")
        entities[1] = "/" + entities[1].replace(".", "/")
        # print entities
        entities_with_facts.add(tuple(entities))
    
    sys.stderr.write("No. of facts loaded: %d\n" %(len(entities_with_facts)))
    
    for line in sys.stdin:
        line = line.rstrip()
        parts = json.loads(line)
        
        # print parts
        check = False
        for word_index in range(1, len(parts)):
            for j in range(word_index + 1, len(parts)):
                key = (parts[word_index][0], parts[j][0])
                reverse_key = (parts[j][0], parts[word_index][0])
                if key in entities_with_facts or reverse_key in entities_with_facts:
                    print line
                    check = True
                    break
            if check:
                break
    
if __name__ == "__main__":
    facts_file = sys.argv[1]
    filter_sentences(facts_file)