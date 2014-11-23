'''
Created on 25 Apr 2014

Assumption: Each sentence has one named entity
    1. First attempt is to use maximum string match to find the entity
    2. If 1 fails, use NE recognizer


@author: siva
'''


import sys
import re
import json

word_list_file = sys.argv[1]
word_count_max = int(sys.argv[2])
word_list = set()
word_list.add("how-many")
word_count = 0
for word in open(word_list_file):
    word = word.strip().split("\t")[1].lower()
    if word in word_list:
       continue
    word_list.add(word)
    word_count += 1
    if word_count > word_count_max:
        break

key_to_mid_file = "data/freebase/lexicon/key_to_mid.txt"
key_to_mid = {}
for line in open(key_to_mid_file):
    line = line.strip().split("\t")
    key = line[0]
    mid = line[1]
    key_to_mid[key] = mid

for line in sys.stdin:
    line = json.loads(line)
    # print line
    entity = line['url'].split('/')[-1]
    entity_mid = key_to_mid.get(entity, "out-of-domain")
    entity_words = set(entity.split("_"))
    
    words = [word['word'] for word in line['words']]
    word_elements = line["words"]
    spans = []
    
    span_check = False
    span_start = -1
    span_start = -1
    for i, word in enumerate(words):
        if (word in entity_words) or (word_elements[i]['ner'] != "O" and word_elements[i]['ner'] != 'DATE') or (word.lower() not in word_list and not re.match("[0-9]+$", word)):
            if not span_check:
                span_start = i
                span_end = i
                span_check = True
            else:
                span_end += 1
        else:
            if span_check:
                spans.append((span_start, span_end))
            span_check = False
    best_span_size = -1
    best_span = [-1, -1]
    for span_start, span_end in spans:
        span_size = span_end - span_start
        if best_span_size < span_size:
            best_span_size = span_size
            best_span = [span_start, span_end]
    if best_span_size != -1:
        left = best_span[0]
        right = best_span[1]
        entity = ""
        ner = "O"
        word_elements = line['words']
        new_word_elements = word_elements[0:left]
        for i in range(left, right + 1):
            if word_elements[i]['ner'] != "O" and word_elements[i]['ner'] != 'DATE':
                ner = word_elements[i]['ner']
            word = words[i] 
            entity += word[0].upper() + word[1:] + "_"
        entity = entity.strip("_")
        new_word_elements.append({"word" : entity, "ner" : ner})
        new_word_elements += word_elements[right + 1:]
        entity = entity.strip()
        line['words'] = new_word_elements
        line["entities"] = [{"index": left, "entity": entity_mid}]
    else:
        line["entities"] = [{"index": -1, "entity": "out-of-domain"}]
    print json.dumps(line)