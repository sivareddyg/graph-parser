'''
Created on 5 Sep 2013

@author: Siva Reddy
'''

import sys
import json

def process_ner(line):
    sent = json.loads(line)
    prev_entity = "0"
    new_words = []
    old_to_new = {}
    j = 0
    new_word = ""
    i = 0
    words = sent['words']
    while i < len(words):
        # print words[i]['ner']
        try:    
            if words[i]['ner'] == 'O':
                new_words.append(words[i])
                old_to_new[i] = j
                i += 1
                j += 1
            else:
                prev_entity = words[i]['ner']
                cur_entity = words[i]['ner']
                new_word = []
                while cur_entity == prev_entity:
                    old_to_new[i] = j
                    new_word.append(words[i]['word'])
                    i += 1
                    if i >= len(words):
                        break
                    prev_entity = cur_entity
                    cur_entity = words[i]['ner']
                new_words.append({'word' : "_".join(new_word), 'ner' : prev_entity})
                j += 1
        except KeyError:
            sys.stderr.write(line);
            exit()
            
    for entity in sent['entities']:
        entity['index'] = old_to_new[entity['index']]
    sent['words'] = new_words
    print json.dumps(sent)

for line in sys.stdin:
    process_ner(line)