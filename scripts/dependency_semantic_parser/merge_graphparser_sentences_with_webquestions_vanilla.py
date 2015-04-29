'''
Created on Nov 25, 2014

@author: sivareddy
'''

import json
import sys


original_file = sys.argv[1]
sentences = {}
original_sentence_order = []
for line in open(original_file):
    sentence = json.loads(line)
    sentence_text = " ".join([word['word']
                              for word in sentence['words']]).lower().strip()
    sentences[sentence_text] = sentence
    original_sentence_order.append(sentence_text)

new_sentences = {}
for line in sys.stdin:
    sent = json.loads(line)
    sent_text = " ".join([word['word']
                          for word in sent['words']]).lower().strip()
    if sent_text in sentences:
        org_sent = sentences[sent_text]
        for key in org_sent:
            if key not in sent:
                sent[key] = org_sent[key]
        new_sentences[sent_text] = sent

for sent_text in original_sentence_order:
    if sent_text in new_sentences:
        print json.dumps(new_sentences[sent_text])
    else:
        print json.dumps(sentences[sent_text])
