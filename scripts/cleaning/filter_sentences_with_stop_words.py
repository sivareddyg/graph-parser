'''
Created on 23 Aug 2013

@author: Siva Reddy
'''

import json
import sys
import re

entity_file = sys.argv[1]
sentence_minimum_length = 3 #words

# valid entities
entities = set()

stop_words = set()
sentence_start_stop_words = set()

uppercase = re.compile("[A-Z]")

for line in open("data/stop_words.txt"):
    line = line.strip()
    if line == "" or line[0] == "#":
        continue
    word = line.split()[0]
    stop_words.add(word)

for line in open("data/sentence_starting_stop_words.txt"):
    line = line.strip()
    if line == "" or line[0] == "#":
        continue
    word = line.split()[0]
    sentence_start_stop_words.add(word)

for entity in open(entity_file):
    entity = entity.strip().split()[0]
    entity = entity.replace("ns:", "/").replace(".", "/")
    # print entity
    entities.add(entity)

count = 0
million = 10 ** 6
for line in sys.stdin:
    count += 1
    if count % million == 0:
        sys.stderr.write("Line count: %d\n" %(count))
    line = line.rstrip()
    parts = json.loads(line)
    sentence = parts[0]
    sentence = sentence.strip().split()
    
    # minimum number of words in sentence
    if len(sentence) < sentence_minimum_length:
        continue
    # sentence should start with upper case
    if not uppercase.match(sentence[0][0]):
        continue
    # does not contain ceratin words in the starting
    if sentence[0].lower() in sentence_start_stop_words:
        continue
    
    # check if the sentence contain specific stop words
    for word in sentence:
        word = word.strip().lower()
        if word in stop_words:
            valid = False
            break
    
    if not valid:
        continue
    
    print line