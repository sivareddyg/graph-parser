import json
import sys

ner_mapping = {"LUG": "LOCATION", "PERS": "PERSON",
               "ORG": "ORGANIZATION", "OTROS": "ORGANIZATION"}
for line in sys.stdin:
    line = json.loads(line)
    old_to_new_index = {}
    index = 0
    new_index = 0
    new_words = []
    words = line['words']
    new_entities = []
    for entity in line['entities']:
        if index > entity['start']:
            continue
        while index < entity['start']:
            old_to_new_index[index] = new_index
            new_words.append(words[index])
            index += 1
            new_index += 1
        entity_words = []
        # print words, index, entity
        new_word = words[index]
        while index < entity['end'] + 1:
            old_to_new_index[index] = new_index
            entity_words.append(words[index]['word'])
            index += 1
        new_word['word'] = "_".join(entity_words)
        if 'ner' in entity:
            new_word['ner'] = ner_mapping[entity['ner']]
            del entity['ner']
        new_word['pos'] = "np00000"
        new_words.append(new_word)
        del entity['start']
        del entity['end']
        entity['index'] = new_index
        new_index += 1
        if 'entity' in entity:
            new_entities.append(entity)
    while index < len(line['words']):
        new_words.append(words[index])
        index += 1
    # print json.dumps(line)
    line['words'] = new_words
    line['entities'] = new_entities
    print json.dumps(line)
