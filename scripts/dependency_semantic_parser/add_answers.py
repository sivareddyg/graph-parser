'''
Created on Nov 25, 2014

@author: sivareddy
'''

import json
import sys


original_file = sys.argv[1]
sentences = {}
for line in open(original_file):
    sentence = json.loads(line)
    sentence_text = " ".join([word['word']
                              for word in sentence['words']]).lower().strip()
    sentences[sentence_text] = sentence

for line in sys.stdin:
    sent = json.loads(line)
    sent_text = " ".join([word['word']
                          for word in sent['words']]).lower().strip()
    if sent_text in sentences:
        org_sent = sentences[sent_text]
        for key in org_sent:
            if key == "synPars":
                continue
            if key not in sent:
                sent[key] = org_sent[key]
        print json.dumps(sent)