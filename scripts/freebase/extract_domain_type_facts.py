'''
Created on 29 Aug 2013

@author: Siva Reddy
'''

import re
import sys

entities = set()
def extract_domain_type_facts(entity_lexicon_file):
    for line in open(entity_lexicon_file):
        entity = line.split('\t')[0]
        entities.add(entity)
        
    for line in sys.stdin:
        parts = line.strip().split('\t')
        parts[2] = parts[2].strip(".")
        if parts[0] in entities or parts[2] in entities:
            sys.stdout.write(" ".join(parts) + " .\n")
            
if __name__ == "__main__":
    extract_domain_type_facts(sys.argv[1])