'''
Created on 23 Aug 2013

@author: Siva Reddy

Reads sentences extracted from Clueweb annotated with Freebase.

'''

import sys
import json
import re

entity_file = sys.argv[1]
# valid entities
entities = {}

# lexicon = {word : {entity : count}}
lexicon = {}



for entity in open(entity_file):
    entity = entity.strip().split()[0]
    entities[entity] = 1

count = 0
million = 1000000
for line in sys.stdin:
    count += 1
    if count % million == 0:
        sys.stderr.write("Line count: %d\n" %(count))
    line = line.rstrip()
    parts = json.loads(line)
    sentence = parts[0]
    entities_sentence = parts[1:]
    for entity_sentence in entities_sentence:
        # print entity_sentence
        entity = entity_sentence[0]
        entity = entity.replace("/m", "ns:m").replace("/", ".")
        if not entities.has_key(entity):
            continue
        entity = entity.encode("utf-8")
        start = entity_sentence[2]
        end = entity_sentence[3]
        word = sentence[start:end].lower()
        word = re.sub("[\s]+", " ", word)
        word = word.strip()
        word = word.encode("utf-8", "ignore")
        if word == "":
            continue
        # print entity
        if not lexicon.has_key(word):
            lexicon[word] = {}
            lexicon[word]["COUNT"] = 0
        if not lexicon[word].has_key(entity):
            lexicon[word][entity] = 0
        lexicon[word][entity] += 1
        lexicon[word]["COUNT"] += 1

words = lexicon.items()
words.sort(key = lambda x : x[1]["COUNT"], reverse = True)

for word, entities in words:
    del entities["COUNT"]
    scores = entities.items()
    scores.sort(key = lambda x : x[1], reverse = True)
    for entity, score in scores:
        print "%s\t%s\t%d" %(word, entity, score)