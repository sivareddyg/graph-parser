# -*- coding: utf-8 -*-
'''

Populates NER information into entities.

Input: {"entities": [{"start": 26, "end": 26, "name": "Viena", "entity": "m.0fhp9"}, {"start": 29, "end": 29, "name": "Austria", "entity": "m.0h7x"}], "words": [{"word": "Hasta", "ner": "O", "pos": "sp000"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "temporada", "ner": "O", "pos": "nc0s000"}, {"word": "1949-1950", "ner": "O", "pos": "z0"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "este", "ner": "O", "pos": "dd0000"}, {"word": "cambi\u00f3", "ner": "O", "pos": "vmis000"}, {"word": "a", "ner": "O", "pos": "sp000"}, {"word": "\"", "ner": "O", "pos": "fe"}, {"word": "Liga", "ner": "OTROS", "pos": "np00000"}, {"word": "nacional", "ner": "O", "pos": "aq0000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "f\u00fatbol", "ner": "O", "pos": "nc0s000"}, {"word": "profesional", "ner": "O", "pos": "aq0000"}, {"word": "\"", "ner": "O", "pos": "fe"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "sin", "ner": "O", "pos": "sp000"}, {"word": "embargo", "ner": "O", "pos": "nc0s000"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "exclusi\u00f3n", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "parte", "ner": "O", "pos": "nc0s000"}, {"word": "exterior", "ner": "O", "pos": "aq0000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "Viena", "ner": "ORG", "pos": "np00000"}, {"word": "y", "ner": "O", "pos": "cc"}, {"word": "Baja", "ner": "ORG", "pos": "np00000"}, {"word": "Austria", "ner": "ORG", "pos": "np00000"}, {"word": "a", "ner": "O", "pos": "sp000"}, {"word": "cabo", "ner": "O", "pos": "nc0s000"}, {"word": "bajo", "ner": "O", "pos": "sp000"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "por", "ner": "O", "pos": "sp000"}, {"word": "lo", "ner": "O", "pos": "da0000"}, {"word": "que", "ner": "O", "pos": "pr000000"}, {"word": "gran", "ner": "O", "pos": "aq0000"}, {"word": "parte", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "el", "ner": "O", "pos": "da0000"}, {"word": "interior", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "el", "ner": "O", "pos": "da0000"}, {"word": "pa\u00eds", "ner": "O", "pos": "nc0s000"}, {"word": "no", "ner": "O", "pos": "rn"}, {"word": "jugaba", "ner": "O", "pos": "vmii000"}, {"word": ".", "ner": "O", "pos": "fp"}], "sentence": "Hasta la temporada 1949-1950 , este cambi\u00f3 a \" Liga nacional de f\u00fatbol profesional \" , sin embargo , la exclusi\u00f3n de la parte exterior de Viena y Baja Austria a cabo bajo , por lo que gran parte de el interior de el pa\u00eds no jugaba ."}

Output: {"entities": [{"start": 9, "ner": "OTROS", "end": 9, "name": "Liga"}, {"start": 26, "ner": "ORG", "end": 26, "name": "Viena", "entity": "m.0fhp9"}, {"start": 29, "end": 29, "name": "Austria", "entity": "m.0h7x"}], "words": [{"word": "Hasta", "ner": "O", "pos": "sp000"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "temporada", "ner": "O", "pos": "nc0s000"}, {"word": "1949-1950", "ner": "O", "pos": "z0"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "este", "ner": "O", "pos": "dd0000"}, {"word": "cambi\u00f3", "ner": "O", "pos": "vmis000"}, {"word": "a", "ner": "O", "pos": "sp000"}, {"word": "\"", "ner": "O", "pos": "fe"}, {"word": "Liga", "ner": "OTROS", "pos": "np00000"}, {"word": "nacional", "ner": "O", "pos": "aq0000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "f\u00fatbol", "ner": "O", "pos": "nc0s000"}, {"word": "profesional", "ner": "O", "pos": "aq0000"}, {"word": "\"", "ner": "O", "pos": "fe"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "sin", "ner": "O", "pos": "sp000"}, {"word": "embargo", "ner": "O", "pos": "nc0s000"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "exclusi\u00f3n", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "la", "ner": "O", "pos": "da0000"}, {"word": "parte", "ner": "O", "pos": "nc0s000"}, {"word": "exterior", "ner": "O", "pos": "aq0000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "Viena", "ner": "ORG", "pos": "np00000"}, {"word": "y", "ner": "O", "pos": "cc"}, {"word": "Baja", "ner": "ORG", "pos": "np00000"}, {"word": "Austria", "ner": "ORG", "pos": "np00000"}, {"word": "a", "ner": "O", "pos": "sp000"}, {"word": "cabo", "ner": "O", "pos": "nc0s000"}, {"word": "bajo", "ner": "O", "pos": "sp000"}, {"word": ",", "ner": "O", "pos": "fc"}, {"word": "por", "ner": "O", "pos": "sp000"}, {"word": "lo", "ner": "O", "pos": "da0000"}, {"word": "que", "ner": "O", "pos": "pr000000"}, {"word": "gran", "ner": "O", "pos": "aq0000"}, {"word": "parte", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "el", "ner": "O", "pos": "da0000"}, {"word": "interior", "ner": "O", "pos": "nc0s000"}, {"word": "de", "ner": "O", "pos": "sp000"}, {"word": "el", "ner": "O", "pos": "da0000"}, {"word": "pa\u00eds", "ner": "O", "pos": "nc0s000"}, {"word": "no", "ner": "O", "pos": "rn"}, {"word": "jugaba", "ner": "O", "pos": "vmii000"}, {"word": ".", "ner": "O", "pos": "fp"}], "sentence": "Hasta la temporada 1949-1950 , este cambi\u00f3 a \" Liga nacional de f\u00fatbol profesional \" , sin embargo , la exclusi\u00f3n de la parte exterior de Viena y Baja Austria a cabo bajo , por lo que gran parte de el interior de el pa\u00eds no jugaba ."}

Created on 12 Feb 2015

@author: siva
'''

import json
import sys


def populate_ner(ner_start, ner_end, ner_type, position_to_entity_map, sentence):
    if ner_type == 'O' or ner_start == -1 or ner_end == -1:
        return
    entities = sentence['entities']
    words = sentence['words']
    if ner_start in position_to_entity_map:  # ner_start has an entity
        entity = position_to_entity_map[ner_start]
        if ner_start == entity['start'] and ner_end == entity['end']:
            entity['ner'] = ner_type
    elif ner_end in position_to_entity_map:  # entity_end has an entity
        entity = position_to_entity_map[ner_end]
        if ner_start == entity['start'] and ner_end == entity['end']:
            entity['ner'] = ner_type
    else:  # ner is not an entity
        phrase = " ".join(word['word']
                          for word in words[ner_start: ner_end + 1])
        entity = {'start': ner_start, 'end': ner_end,
                  'ner': ner_type, 'name': phrase}
        entities.append(entity)
        entities.sort(key=lambda x: x['start'])


def create_position_to_entity_map(entities):
    position_to_entity_map = {}
    for entity in entities:
        for i in range(entity['start'], entity['end'] + 1):
            position_to_entity_map[i] = entity
    return position_to_entity_map


def process_sentence(sentence):
    words = sentence['words']
    entities = sentence['entities']
    position_to_entity_map = create_position_to_entity_map(entities)
    prev_ner = 'O'
    cur_ner = 'O'
    ner_start = -1
    ner_end = -1
    ner_type = "O"
    for i, word in enumerate(words):
        cur_ner = word['ner']
        if cur_ner != 'O':
            if prev_ner == cur_ner:
                ner_end = i
            else:
                populate_ner(
                    ner_start, ner_end, ner_type, position_to_entity_map, sentence)
                ner_start = i
                ner_end = i
                ner_type = cur_ner
        else:
            populate_ner(
                ner_start, ner_end, ner_type, position_to_entity_map, sentence)
            ner_start = -1
            ner_end = -1
            ner_type = 'O'
        prev_ner = cur_ner

    populate_ner(
        ner_start, ner_end, ner_type, position_to_entity_map, sentence)

    for word in words:
        if 'ner' in word:
            del word['ner']

if __name__ == '__main__':
    for line in sys.stdin:
        sentence = json.loads(line)
        process_sentence(sentence)
        print json.dumps(sentence)
