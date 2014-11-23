'''
Created on 4 Sep 2013

@author: Siva Reddy
'''

import json
import sys
import re

# [" Select an online photo storage service, such as Snapfish by HP or 
# Kodak EasyShare, and create an account.", ["/m/08f0xv", 1.0, 49, 57], ["/m/03mnk", 0.935254, 61, 63], ["/m/07n8g5", 1.0, 67, 82]]

for line in sys.stdin:    
    line = line[:-1]
    if line == "":
        continue
    line = json.loads(line)
    sent = line[0]
    sent_appended = ""
    prev_entity_end = 0
    for entity in line[1:]:
        entity_start = entity[2]
        entity_end = entity[3]
        sent_appended += sent[prev_entity_end:entity_start]
        entity_string = sent[entity_start:entity_end]
        sent_appended += " ENTITY_START " + entity_string + " ENTITY_END "
        prev_entity_end = entity_end
    sent_appended += sent[prev_entity_end:]
    print re.sub("[\s]+", " ", sent_appended.strip())