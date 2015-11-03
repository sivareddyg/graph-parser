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
MAX_NBEST = 10

total = 0
for line in sys.stdin:
    sentence = json.loads(line)
    if 'index' in sentence:
        sentence_id = sentence['index']
    else:
        sentence_id = total
    sentence_ids.add(sentence_id)
    sentence_id_to_sentence[sentence_id] = sentence['sentence']
    total += 1
    if sentence_id in answer_found_sentences:
        continue
    if 'disambiguatedEntities' not in sentence:
        no_predictions.add(sentence_id)
    else:
        if len(sentence['disambiguatedEntities']) == 0:
            no_predictions.add(sentence_id)
        index_count = 1
        for entities in sentence['disambiguatedEntities']:
            entity_ids = set()
            for entity in entities['entities']:
                if "entity" in entity:
                    entity_ids.add(entity["entity"])
                if "id" in entity:
                    entity_ids.add(entity["id"].split("/")[-1])
            # print entity_ids
            gold_entity = sentence["goldMid"]
            # gold_entity_id = sentence["url"].split("/")[-1]
            if len(entity_ids) == 0:
                print sentence['sentence']
            if gold_entity in entity_ids:  # or gold_entity_id in entity_ids:
                answer_found_sentences.add(sentence_id)
                if index_count not in sentence_position_to_accuracy:
                    sentence_position_to_accuracy[index_count] = 0
                sentence_position_to_accuracy[index_count] += 1
                break
            index_count += 1
            if index_count > MAX_NBEST:
                break

print "total =", len(sentence_ids)
print "nthBest\tcount\ttotalAccuracy"

positives = 0.0
for i in range(1, MAX_NBEST + 1):
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
