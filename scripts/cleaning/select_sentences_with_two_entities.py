'''
Created on 23 Aug 2013

@author: Siva Reddy
'''

import json
import sys
import re

entity_file = sys.argv[1]
sentence_minimum_length = 3 #words

# valid entities
entities = set()

uppercase = re.compile("[A-Z]")


for entity in open(entity_file):
    entity = entity.strip().split()[0]
    entity = entity.replace("ns:", "/").replace(".", "/")
    # print entity
    entities.add(entity)

count = 0
million = 10 ** 6
for line in sys.stdin:
    count += 1
    if count % million == 0:
        sys.stderr.write("Line count: %d\n" %(count))
    line = line.rstrip()
    parts = json.loads(line)
    #sentence = parts[0]
    sentence = [word['word'] for word in parts['words']]
    sentence = sentence.strip().split()
    
    # minimum number of words in sentence
    if len(sentence) < sentence_minimum_length:
        continue
    # sentence should start with upper case
    if not uppercase.match(sentence[0][0]):
        continue
    
    # At least two entities from the domain
    # entities_sentence = parts[1:]
    entities_sentence = [entity['entity'] for entity in parts['entities']]
    if len(entities_sentence) < 2:
        continue
    valid = True
    sent_entities = []    
    for entity_sentence in entities_sentence:
        entity = entity_sentence[0]
        if not entity in entities:
            valid = False
            break
        if entity not in sent_entities:
            sent_entities.append(entity)
    if len(sent_entities) < 2:
        continue
    if not valid:
        continue
    # print sentence
    
    print line