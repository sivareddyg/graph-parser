import sys
import json
for line in sys.stdin:
    word_to_mention = {}
    line = json.loads(line)
    word_index = 0
    sent = []
    entity_mapping = {}
    if 'entities' in line:
        for entity in line['entities']:
            entity_mapping[entity['index']] = entity
    for original_index, word in enumerate(line['words']):
        if ("ner" in word and word['ner'] != "O" and word['ner'] != "0") or (original_index in entity_mapping):
            word_parts = []
            if "ner" in word and word['ner'] == "DATE":
                word_parts = word['word'].split("_")
            else:
                word_parts = [word_part[0].upper() + word_part[1:] for word_part in word['word'].split("_")]
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
    if "targetValue" in line:
        sentence['targetValue'] = line['targetValue']
    if "answer" in line:
        sentence['answer'] = line['answer']
    #if len(sentence["entities"]) > 0:
    print json.dumps(sentence)
    # print sent, word_to_mention
