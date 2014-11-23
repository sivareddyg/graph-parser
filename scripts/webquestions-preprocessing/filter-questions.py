'''
Created on 30 Apr 2014

@author: siva
'''

import sys
import json
import re

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

# print word_list

for line in sys.stdin:
    line = line.strip()
    parts = json.loads(line)
    entity_index = set([entity['index'] for entity in parts['entities']])
    is_useful = True
    
    targetValue = parts["targetValue"]
    if re.search("(([0-9]{4} [A-Za-z]+)|(description [A-Za-z]+ [0-9]{4}))", targetValue):
        is_useful = False
    
    if not is_useful:
        sys.stderr.write(targetValue + "\n")
        sys.stderr.write(parts['sentence'])
        sys.stderr.write("\n")
        continue
    
    for i, word in enumerate(parts['words']):
        if i in entity_index:
            continue
        word = word['word'].lower()
        if re.match("[0-9]+$", word):
           continue 
        if word not in word_list:
            is_useful = False
            break
    if is_useful:
        print line
        # print parts['sentence']
    else:
        sys.stderr.write(parts['sentence'])
        sys.stderr.write("\n")