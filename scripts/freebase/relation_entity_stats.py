'''
Created on 1 May 2014

Get the top 100 relations


@author: siva
'''

import sys
import json
from collections import Counter

def get_relation_types():
    relation_arg_count = Counter()
    
    for line in sys.stdin:
        if line[0] == '#':
            continue
        line = line.strip()
        if  line.strip() == "":
            continue
        line = line.split("\t")
        entities = json.loads(line[0])
        relations = json.loads(line[1])
        
        if type(entities) == type([]):
            entity1 = entities[0]
            entity2 = entities[1]
            if entity1[:2] == "m." and  entity2[:2] == "m.":
                for relation in relations:
                    relation = json.dumps(relation);
                    if str(relation).find("date") > -1:
                        continue
                    relation_arg_count["%s # %s # left_arg" %(entity1, relation)] += 1 
                    relation_arg_count["%s # %s # right_arg" %(entity2, relation)] += 1
        if len(relation_arg_count) > 50000:
            relation_arg_count = Counter(dict(relation_arg_count.most_common(30000)))
            # break
        
    relation_counter = Counter()
    for key, value in relation_arg_count.most_common(100):
        relation = key.split(" # ", 1)[1]
        relation_counter[relation] += value
        # print "%s\t%s" %(key, value)
        
    for key, value in relation_counter.most_common(10):
        print "%s\t%s" %(key, value)
        
if __name__ == "__main__":
    get_relation_types()