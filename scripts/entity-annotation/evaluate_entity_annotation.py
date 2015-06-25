'''
Created on 24 Jun 2015

@author: siva
'''

import json
import sys

sentence_position_to_accuracy = {}
sentence_ids = set()
answer_found_sentences = set()

prev_index = -1
index_count = 0
total = 0
for line in sys.stdin:
    sentence = json.loads(line)
    sentence_id = sentence['index']
    sentence_ids.add(sentence_id)
    if sentence_id in answer_found_sentences:
        continue
    entities = sentence['entities']
    entity_ids = set()
    for entity in entities:
        if "id" in entity:
            entity_ids.add(entity["id"].split("/")[-1])
    gold_entity = sentence["url"].split("/")[-1]
    if len(entity_ids) == 0:
        continue
    if prev_index != sentence_id:
        total += 1
        index_count = 1
    else:
        index_count += 1
    if gold_entity in entity_ids:
        answer_found_sentences.add(sentence_id)
        if index_count not in sentence_position_to_accuracy:
            sentence_position_to_accuracy[index_count] = 0
        sentence_position_to_accuracy[index_count] += 1
    prev_index = sentence_id

print "total =", len(sentence_ids)
print "nthBest\tcount\ttotalAccuracy"

positives = 0.0
for i in range(1,11):
    positives += sentence_position_to_accuracy.get(i, 0);
    print "%d\t%d\t%.3f" % (i, sentence_position_to_accuracy.get(i, 0), positives/ len(sentence_ids))
