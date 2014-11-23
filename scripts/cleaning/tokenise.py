import sys
from unitok import LANGUAGE_DATA, tokenise
import re
import json

lsd = LANGUAGE_DATA['english']()

# input sentence: [" Among the most famous ABC are tennis player Michael Chen and Yahoo co-founder Jerry Yang.", ["/m/0gsg7", 0.840163, 23, 26], ["/m/011zfk", 0.997802, 79, 89], ["/m/019rl6", 0.976942, 62, 67]]

# Output: {"entities": [{"index": 4, "score": 0.840163, "entity": "m.0gsg7"}, {"index": 13, "score": 0.997802, "entity": "m.011zfk"}, {"index": 14, "score": 0.976942, "entity": "m.019rl6"}], "words": [{'word':"Among"}, {'word':"the"}, {'word':"most"}, {'word':"famous"}, {'word':"ABC"}, {'word':"are"}, {'word':"tennis"}, {'word':"player"}, {'word':"Michael"}, {'word':"Chen"}, {'word':"and"}, {'word':"Yahoo"}, {'word':"co-founder"}, {'word':"Jerry_Yang"}}

for line in sys.stdin:    
    line = line[:-1]
    if line == "":
        continue
    line = json.loads(line)
    sent = line[0]
    # print sent
    entities = line[1]
    entities.sort(key = lambda x : x[2])
    sent_appended = ""
    prev_entity_end = 0
    for entity in entities:
        entity_start = entity[2]
        entity_end = entity[3]
        sent_appended += sent[prev_entity_end:entity_start]
        entity_string = sent[entity_start:entity_end]
        sent_appended += " ENTITY_START " + entity_string + " ENTITY_END "
        prev_entity_end = entity_end
    sent_appended += sent[prev_entity_end:]
    sent_appended = sent_appended.strip()
    
    # print sent_appended
    
    sent_appended = re.sub("[\s]+", " ", sent_appended.strip())
    # sent ={}
    
    tokens = tokenise(sent_appended, lsd, None)
    
    new_sent = []
    entity = []
    word_index = 0
    entity_count = 0
    entity_items = []
    check = False
    for token in tokens:
        if token == "ENTITY_END":
            # print entity
            new_sent.append({'word' : "_".join(entity)})
            entity_dict = {}
            entity_dict['entity'] = entities[entity_count][0][1:].replace("/", ".")
            entity_dict['score'] = entities[entity_count][1]
            entity_dict['index'] = word_index
            entity_items.append(entity_dict)
            entity = []
            check = False
            word_index += 1
            entity_count += 1
            continue
        if token == "ENTITY_START":
            entity = []
            check = True
            continue
        if check:
            entity.append(token)
        else:
            new_sent.append({'word' : token })
            word_index += 1
    
    sent = {}
    sent['words'] = new_sent
    sent['entities'] = entity_items
    print json.dumps(sent)
