# -*- coding: utf-8 -*-
'''
Created on 21 Jan 2015

# TODO(sivareddyg): zip file loading is annoyingly slow in Python. Consider to use Java instead.

@author: siva
'''

import json
import os
import re
import string
import sys

MIN_ENTITY_LENGTH = 4
MAX_ENTITY_AMBIGUITY = 3
NUMBERS_PATTERN = re.compile("[0-9]+$")


def load_name_to_entity_dict(entity_file_name):
    name_to_entity_dict = {}
    for line in os.popen("zcat %s" % (entity_file_name)):
        (entity, name) = line.split(" ", 1)
        name = name[0:name.rfind("@")]
        name = name.strip('"')
        if len(name) < MIN_ENTITY_LENGTH:
            continue
        if NUMBERS_PATTERN.match(name):  # if the entity is a number
            continue
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


def load_kb(facts_file_name, schema_file_name):
    types_all = {}
    relations_all = {}
    f = open(schema_file_name)
    line = f.readline()
    while line != "":
        line = line.rstrip()
        if line == "":
            line = f.readline()
            continue
        if line[0] != "\t":
            parts = line.strip().split('\t')
            parent = parts[0]
            parent_type = parts[1]
            types_all[parent] = parent_type
            line = f.readline()
            while line != "" and line.strip() != "":
                if line[0] == '\t':
                    parts = line.strip().split('\t')
                    rel = parts[0]
                    child = parts[1]
                    rel_inv = parts[3]
                    # print line
                    relations_all[rel] = (parent, child)
                    relations_all[rel_inv] = (child, parent)
                line = f.readline()
    f.close()

    entities_with_facts = set()
    for line in os.popen("zcat %s" % (facts_file_name)):
        if line[0] == "#":
            continue
        line = line.strip()
        if line == "":
            continue
        if line[0] != "[":
            continue

        # sentences are extracted using binary relations formed by two entities
        # from the domain
        entities = json.loads(line.split('\t')[0])
        relations = json.loads(line.split('\t')[1])
        if len(entities) > 1 and entities[0].startswith("m.") and entities[1].startswith("m."):
            for relation in relations:
                check = False
                for relation_part in relation:
                    if relation_part in relations_all:
                        entity1 = "/" + entities[0].replace(".", "/")
                        entity2 = "/" + entities[1].replace(".", "/")
                        entry = (
                            entity1.strip("/").replace("/", "."), entity2.strip("/").replace("/", "."))
                        entities_with_facts.add(entry)
                        check = True
                        break
                if check:
                    break
    sys.stderr.write("No. of facts loaded: %d\n" % (len(entities_with_facts)))
    return entities_with_facts


def process_sentence(sent, name_to_entity_dict, facts):
    entities = []
    entity_maps = []
    words = sent.strip().split()
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
                entity_map = {"start": index, "end": last_entities_matched_index, "name": " ".join(
                    words[index:last_entities_matched_index + 1])}
                entity_maps.append(entity_map)
                entities.append(last_entities_matched)
            index += last_entities_matched_index - index
        index += 1
    if len(entities) > 1 and len(entities) < 5:
        disambiguated_entities = disamabiguate(entities, facts)
        final_entities = []
        sentence_map = {}
        if disambiguated_entities[1] > 0:
            for i, entity_map in enumerate(entity_maps):
                if disambiguated_entities[2][i] == 1:
                    entity_map['entity'] = disambiguated_entities[0][i]
                    final_entities.append(entity_map)
            sentence_map['entities'] = final_entities
            sentence_map['sentence'] = " ".join(words)
            print json.dumps(sentence_map)


def generate_all_combinations(list_of_list_of_entities):
    if list_of_list_of_entities == []:
        return [[]]

    new_combinations = []
    combinations_so_far = generate_all_combinations(
        list_of_list_of_entities[1:])

    for entity in list_of_list_of_entities[0]:
        for combination in combinations_so_far:
            new_combinations.append([entity] + combination)
    return new_combinations


def get_combination_score(entity_list, facts):
    score = 0
    valid_entities = [0] * len(entity_list)
    for i, entity1 in enumerate(entity_list):
        for j, entity2 in enumerate(entity_list[i + 1:]):
            if (entity1, entity2) in facts or (entity2, entity1) in facts:
                valid_entities[i] = 1
                valid_entities[i + j + 1] = 1
                score += 1
    return (score, valid_entities)


def disamabiguate(entities, facts):
    combinations = generate_all_combinations(entities)
    pairs = []
    for combination in combinations:
        score, valid_entities = get_combination_score(combination, facts)
        pairs.append((combination, score, valid_entities))
    pairs.sort(key=lambda x: x[1], reverse=True)
    if pairs != []:
        return pairs[0]


def main():
    name_to_entity = load_name_to_entity_dict(sys.argv[1])
    facts = load_kb(sys.argv[2], sys.argv[3])
    # name_to_entity["w:Asociación"]["w:Mexicana"]["w:de"]["w:Productores"]["w:de"]["w:Fonogramas"]["w:y"]["w:Videogramas"]["w:A."]
    # line = '''Asociación Mexicana de Productores de Fonogramas y Videogramas , A. C. , he.'''
    # line = '''El Darwin College de la Universidad de Cambridge , fundado en 1964.'''
    for line in sys.stdin:
        process_sentence(line, name_to_entity, facts)

if __name__ == '__main__':
    main()
