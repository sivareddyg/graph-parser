'''
Created on 21 May 2014

@author: siva
'''

import json
import sys
from collections import Counter

word_mapping = {}
for line in sys.stdin:
    line = line.strip()
    parts = json.loads(line)
    words = parts['words']
    for entity in parts['entities']:
        word = words[entity['index']]['word'].lower().strip()
        word = " ".join(word.split("_"))
        word_entity_set = word_mapping.get(word, [])
        word_entity_set.append(entity['entity'])
        word_mapping[word] = word_entity_set

for word in word_mapping:
    entity = Counter(word_mapping[word]).most_common(1)[0][0]
    print "%s :- NP : %s" %(word, entity)  