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

print "nthBest\tcount\ttotalAccuracy"
print "1\t%d\t%.3f" % (sentence_position_to_accuracy[1], (sentence_position_to_accuracy[1] + 0.0) / len(sentence_ids))
print "2\t%d\t%.3f" % (sentence_position_to_accuracy[2], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + 0.0) / len(sentence_ids))
print "3\t%d\t%.3f" % (sentence_position_to_accuracy[3], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + 0.0) / len(sentence_ids))
print "4\t%d\t%.3f" % (sentence_position_to_accuracy[4], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + 0.0) / len(sentence_ids))
print "5\t%d\t%.3f" % (sentence_position_to_accuracy[5], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + 0.0) / len(sentence_ids))
print "6\t%d\t%.3f" % (sentence_position_to_accuracy[6], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + sentence_position_to_accuracy[6] + 0.0) / len(sentence_ids))
print "7\t%d\t%.3f" % (sentence_position_to_accuracy[7], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + sentence_position_to_accuracy[6] + sentence_position_to_accuracy[7] + 0.0) / len(sentence_ids))
print "8\t%d\t%.3f" % (sentence_position_to_accuracy[8], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + sentence_position_to_accuracy[6] + sentence_position_to_accuracy[7] + sentence_position_to_accuracy[8] + 0.0) / len(sentence_ids))
print "9\t%d\t%.3f" % (sentence_position_to_accuracy[9], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + sentence_position_to_accuracy[6] + sentence_position_to_accuracy[7] + sentence_position_to_accuracy[8] + sentence_position_to_accuracy[9] + 0.0) / len(sentence_ids))
print "10\t%d\t%.3f" % (sentence_position_to_accuracy[10], (sentence_position_to_accuracy[1] + sentence_position_to_accuracy[2] + sentence_position_to_accuracy[3] + sentence_position_to_accuracy[4] + sentence_position_to_accuracy[5] + sentence_position_to_accuracy[6] + sentence_position_to_accuracy[7] + sentence_position_to_accuracy[8] + sentence_position_to_accuracy[9] + sentence_position_to_accuracy[10] + 0.0) / len(sentence_ids))
