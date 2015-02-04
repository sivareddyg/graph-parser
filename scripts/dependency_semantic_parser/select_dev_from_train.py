'''

Selects development sentences from complete training file

Created on 4 Feb 2015

@author: siva
'''

import json
import sys

dev_sentences = set()
# GraphParser format dev data
for line in open(sys.argv[1]):
    sent = json.loads(line)
    dev_sentences.add(sent["sentence"])

# Dep Parser format training data
for line in open(sys.argv[2]):
    sent = json.loads(line)
    sentence = sent["sentence"]
    if sentence in dev_sentences:
        print line.strip()