'''
Created on 28 Jul 2015

@author: siva
'''

import json
import sys

sentence_to_gold_relations = {}
for line in open(sys.argv[1]):
    sentence = json.loads(line)
    if 'sentence' in sentence and 'goldRelations' in sentence:
        sentence_to_gold_relations[
            sentence['sentence']] = sentence['goldRelations']

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "": continue
    sentence = json.loads(line)
    if sentence['sentence'] in sentence_to_gold_relations:
        sentence['goldRelations'] = sentence_to_gold_relations[
            sentence['sentence']]
    print json.dumps(sentence)
