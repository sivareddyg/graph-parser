'''
Created on 21 Jan 2015

@author: siva
'''

import gzip
import json
import sys


def load_name_to_entity_dict(entity_file_name):
    """ Hello world """
    name_to_entity_dict = {}
    entity_file = gzip.open(entity_file_name, 'rb')
    for line in entity_file:
        (entity, name) = line.split(" ", 1)
        name = name[0:name.rfind("@")]
        name = name.strip('"')
        words = name.split()
        current = name_to_entity_dict
        for word in words:
            word = "w:" + word.lower()
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
    # print relations_all
    # print types_all
    # exit()

    entities_with_facts = set()
    for line in gzip.open(facts_file_name):
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
                    if relations_all.has_key(relation_part) and ((types_all.has_key(relations_all[relation_part][0]) and types_all[relations_all[relation_part][0]] == "main") or (types_all.has_key(relations_all[relation_part][1]) and types_all[relations_all[relation_part][1]] == "main")):
                        entity1 = "/" + entities[0].replace(".", "/")
                        entity2 = "/" + entities[1].replace(".", "/")
                        # print (entity1, entity2)
                        entities_with_facts.add((entity1, entity2))
                        check = True
                        break
                if check:
                    break

    sys.stderr.write("No. of facts loaded: %d\n" % (len(entities_with_facts)))
    return entities_with_facts


def process_sentence(sent, name_to_entity_dict, entities_with_facts):
    entities = []
    words = sent.strip().split()
    for index in range(0, len(words)):
        cur_word = "w:" + words[index].lower()
        last_entities_matched = None
        last_entities_matched_index = -1
        if cur_word in name_to_entity_dict:
            next_word = cur_word
            entity_end = index
            next_word_dict = name_to_entity_dict
            while next_word in next_word_dict:
                if "entities" in next_word_dict:
                    last_entities_matched = next_word_dict["entities"]
                    last_entities_matched_index = entity_end - 1
                next_word_dict = next_word_dict[next_word]
                entity_end += 1
                if entity_end >= len(words):
                    break
                next_word = "w:" + words[entity_end].lower()
        if last_entities_matched:
            entity = {"start": index, "end": last_entities_matched_index,
                      "entities": last_entities_matched}
            index += last_entities_matched_index - index
            entities.append(entity)
    if entities != []:
        print sent
        print entities


def main():
    name_to_entity = load_name_to_entity_dict(sys.argv[1])
    print name_to_entity.items()[0:100]
    # facts = load_kb(sys.argv[2], sys.argv[3])
    facts = set()
    for line in sys.stdin:
        process_sentence(line, name_to_entity, facts)

if __name__ == '__main__':
    main()
