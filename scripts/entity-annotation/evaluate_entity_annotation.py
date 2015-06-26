'''
Created on 24 Jun 2015

@author: siva
'''

import json
import sys

sentence_position_to_accuracy = {}
sentence_ids = set()
answer_found_sentences = set()
no_predictions = set()
sentence_id_to_sentence = {}

prev_index = -1
index_count = 0
total = 0
for line in sys.stdin:
    sentence = json.loads(line)
    sentence_id = sentence['index']
    sentence_ids.add(sentence_id)
    sentence_id_to_sentence[sentence_id] = sentence['sentence']
    if "matchedEntities" in sentence:
        no_predictions.add(sentence_id)
    if sentence_id in answer_found_sentences:
        continue
    entities = sentence['entities']
    entity_ids = set()
    for entity in entities:
        if "entity" in entity:
            entity_ids.add(entity["entity"])
    # print entity_ids
    gold_entity = sentence["goldMid"]
    # print gold_entity
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
for i in range(1, 11):
    positives += sentence_position_to_accuracy.get(i, 0)
    print "%d\t%d\t%.3f" % (i, sentence_position_to_accuracy.get(i, 0), positives / len(sentence_ids))

# print answer_found_sentences
out_of_beam = sentence_ids - answer_found_sentences - no_predictions
print "## Out of beam:", len(out_of_beam)
for sentence_id in out_of_beam:
    print sentence_id, sentence_id_to_sentence[sentence_id]
print

print "## No predictions:", len(no_predictions)
for sentence_id in no_predictions:
    print sentence_id, sentence_id_to_sentence[sentence_id]
