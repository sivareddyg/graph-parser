import sys
import json
import re
for line in sys.stdin:
    word_to_mention = {}
    line = json.loads(line)
    word_index = 0
    words = []
    sent = []
    entity_mapping = {}
    for entity in line['entities']:
        entity_mapping[entity['index']] = entity
    for original_index, word in enumerate(line['words']):
        if ("ner" in word and word['ner'] != "O" and word['ner'] != "0") or (original_index in entity_mapping):
            word_parts = re.sub("_.", lambda x : x.group(0).upper(), word['word']).split("_")
            sent.extend(word_parts)
            word_to_mention[original_index] = {}
            word_to_mention[original_index]["start"] = word_index
            word_to_mention[original_index]["end"] = word_index + len(word_parts) - 1
            if "ner" in word and word['ner'] != "O" and word['ner'] != "0":
                word_to_mention[original_index]["ner"] = word["ner"]
            if original_index in entity_mapping:
                word_to_mention[original_index]["entity"] = entity_mapping[original_index]["entity"]
            word_index += len(word_parts) 
        else:
            sent.append(word['word'])
            word_index += 1
    sentence = {}
    sentence["sentence"] = " ".join(sent)
    sentence["entities"] = [] 
    for word_index in sorted(word_to_mention.keys()):
        sentence["entities"].append(word_to_mention[word_index])
    if len(sentence["entities"]) > 0:
        print json.dumps(sentence)
    # print sent, word_to_mention
