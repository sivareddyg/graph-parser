'''
Created on 18 Apr 2014

@author: Siva Reddy
'''

import sys
import json
import commands
from collections import Counter
import re
from random import shuffle


domain_sentences = {}
domain_pattern = sys.argv[1]

files = commands.getoutput("find %s* -type f" %(domain_pattern)).split("\n")
# print files

for domain_file in files:
    for line in open(domain_file):
        line = line.strip()
        if line == "" or line[0] == '(':
            continue
        domain_name = domain_file.split("/")[-1]
        domain_name = re.sub(".txt", "", domain_name)
        # print line
        domain_sentences[line] = domain_name
        
json_sent_file = sys.argv[2]

domain_sentences["who is the head coach of the pittsburgh steelers"] = "other"

print "@relation    sparse.data\n"

print "@attribute    classid    {business, film, people, other}\n"
# print domain_sentences
word_index = {}
index = 1
training_sentences = []
for line in open(json_sent_file):
    line = json.loads(line)
    sentence = line['utterance']
    sent_words = []
    domain_label = domain_sentences[sentence]
    # print domain_label
    if domain_label not in ["business", "film", "people"]:
        domain_label = "other"
    for word in line['words']:
        word_str = word['word'].lower()
        if re.match("NNP.*", word['pos']) or re.match("CD.*", word['pos']) or not re.match("[a-z\-]+$", word_str):
            continue
        else:
            word_str = word['word'].lower()
            if not word_index.has_key(word_str):
                word_index[word_str] = index
                index += 1
            sent_words.append(word_index[word_str])
    sent_words.append(domain_label)
    training_sentences.append(sent_words)

for word in word_index:
    print "@attribute %s real" %(word)
print

shuffle(training_sentences)
shuffle(training_sentences)

print "@data"
for sentence in training_sentences:
    counter = Counter(sentence[:-1])
    domain_label = sentence[-1]
    data_string = ""
    data_string += "%d %s, " %(0, domain_label)
    for word in sorted(counter.keys()):
        data_string += "%d %d, " %(word, counter[word])
    data_string = data_string.strip(", ")
    #data_string += "%d %s" %(index, domain_label)
    print "{%s}" %(data_string)