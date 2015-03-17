# -*- coding: utf-8 -*-
'''
Created on 21 Jan 2015

@author: siva
'''

import json
import os
import re
import string
import sys

MAX_ENTITY_AMBIGUITY = 3
NUMBERS_PATTERN = re.compile("[0-9]+$")


def load_name_to_entity_dict(entity_file_name):
    name_to_entity_dict = {}
    for line in open(entity_file_name):
        line = line.strip()
        (name, entity) = line.split(" :- NP : ", 1)
        # if NUMBERS_PATTERN.match(name):  # if the entity is a number
        #    continue
        words = name.split()
        current = name_to_entity_dict
        for word in words:
            if word in string.punctuation:  # do not use punctuation.
                continue
            word = "w:" + word
            current = current.setdefault(word, {})
        entities = current.setdefault("entities", set())
        entities.add(entity)
    return name_to_entity_dict


def process_sentence(sent, name_to_entity_dict):
    entities = []
    entity_maps = []
    words = sent.strip().lower().split()
    index = 0
    while index < len(words):
        if words[index] in string.punctuation:
            index += 1
            continue
        cur_word = "w:" + words[index]
        last_entities_matched = None
        last_entities_matched_index = -1
        last_non_punctuation_index = index
        if cur_word in name_to_entity_dict:
            next_word = cur_word
            entity_end = index
            next_word_dict = name_to_entity_dict
            while next_word in next_word_dict:
                if "entities" in next_word_dict:
                    last_entities_matched = next_word_dict["entities"]
                    last_entities_matched_index = last_non_punctuation_index
                last_non_punctuation_index = entity_end
                next_word_dict = next_word_dict[next_word]
                entity_end += 1
                while entity_end < len(words) and words[entity_end] in string.punctuation:
                    entity_end += 1
                if entity_end >= len(words):
                    break
                next_word = "w:" + words[entity_end]
            if "entities" in next_word_dict:
                last_entities_matched = list(next_word_dict["entities"])
                last_entities_matched_index = last_non_punctuation_index
        if last_entities_matched:
            if len(last_entities_matched) <= MAX_ENTITY_AMBIGUITY:
                for i in range(index, last_entities_matched_index + 1):
                    words[i] = words[i][0].upper() + words[i][1:]
                entity_map = {"start": index, "end": last_entities_matched_index, "name": " ".join(
                    words[index:last_entities_matched_index + 1]), "entity": list(last_entities_matched)[0]}
                entity_maps.append(entity_map)
            index += last_entities_matched_index - index
        index += 1
    words[0] = words[0][0].upper() + words[0][1:]
    sentence_map = {}
    sentence_map['entities'] = entity_maps
    sentence_map['sentence'] = " ".join(words)
    print json.dumps(sentence_map)


def main():
    name_to_entity = load_name_to_entity_dict(sys.argv[1])
    # print name_to_entity
    # name_to_entity["w:Asociación"]["w:Mexicana"]["w:de"]["w:Productores"]["w:de"]["w:Fonogramas"]["w:y"]["w:Videogramas"]["w:A."]
    # line = '''Asociación Mexicana de Productores de Fonogramas y Videogramas , A. C. , he.'''
    # line = '''El Darwin College de la Universidad de Cambridge , fundado en 1964.'''
    for line in sys.stdin:
        process_sentence(line, name_to_entity)

if __name__ == '__main__':
    main()
