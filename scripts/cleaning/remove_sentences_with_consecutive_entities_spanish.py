'''
Created on 26 May 2015

@author: siva
'''

import json
import re
import sys

sentences = set()
for line in sys.stdin:
    line = json.loads(line)
    if len(line['words']) > 25:
        continue
    sentence = " ".join(word['word'] for word in line['words'])
    sentence = sentence.lower()
    if sentence in sentences:
        continue
    sentences.add(sentence)
    if re.search("[^\s]+ , [^\s]+ , ", sentence):
        sys.stderr.write(sentence + "\n")
        continue
    if re.search("[^\s]+ , (la|los|las|el|lo|un|una) [^\s]+ , ", sentence):
        sys.stderr.write(sentence + "\n")
        continue
    if re.search("[^\s]+ , [^\s]+ (y|e) ", sentence):
        sys.stderr.write(sentence + "\n")
        continue
    if re.search("[^\s]+ , (la|los|las|el|lo|un|una) [^\s]+ (y|e) ", sentence):
        sys.stderr.write(sentence + "\n")
        continue
    print json.dumps(line)
