import json
import md5
import sys

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
        # or ("ner" in word and word['ner'] != "O" and word['ner'] != "0")
        if original_index in entity_mapping:
            word_parts = []
            if "ner" in word and word['ner'] == "DATE":
                word_parts = word['word'].split("_")
            else:
                word_parts = [word_part[0].upper() + word_part[1:]
                              for word_part in word['word'].split("_")]
            sent.extend(word_parts)
            word_to_mention[original_index] = {}
            word_to_mention[original_index]["start"] = word_index
            word_to_mention[original_index][
                "end"] = word_index + len(word_parts) - 1
            if "ner" in word and word['ner'] != "O" and word['ner'] != "0":
                word_to_mention[original_index]["ner"] = word["ner"]
            if original_index in entity_mapping:
                word_to_mention[original_index][
                    "entity"] = entity_mapping[original_index]["entity"]
                if "score" in entity_mapping[original_index]:
                    word_to_mention[original_index][
                        "score"] = entity_mapping[original_index]["score"]
                if "phrase" in entity_mapping[original_index]:
                    word_to_mention[original_index][
                        "phrase"] = entity_mapping[original_index]["phrase"]
                if "name" in entity_mapping[original_index]:
                    word_to_mention[original_index][
                        "name"] = entity_mapping[original_index]["name"]
                if "id" in entity_mapping[original_index]:
                    word_to_mention[original_index][
                        "id"] = entity_mapping[original_index]["id"]
            word_index += len(word_parts)
        else:
            sent.append(word['word'])
            word_index += 1
    sentence = {}
    if 'index' not in line:
        sentence_index = md5.md5(line['sentence']).hexdigest()
        sentence["index"] = sentence_index
    else:
        sentence_index = line['index']
        sentence["index"] = sentence_index
    sentence["sentence"] = " ".join(sent)
    sentence["entities"] = []
    for word_index in sorted(word_to_mention.keys()):
        sentence["entities"].append(word_to_mention[word_index])
    if "targetValue" in line:
        sentence['targetValue'] = line['targetValue']
    if "answer" in line:
        sentence['answer'] = line['answer']
    # if len(sentence["entities"]) > 0:
    print json.dumps(sentence)
    # print sent, word_to_mention
