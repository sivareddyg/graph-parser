'''
Created on 26 May 2015

@author: siva
'''

import json
import re
import sys


for line in sys.stdin:
    sent = json.loads(line)
    entity_indices = set()
    if "entities" in sent:
        for entity in sent['entities']:
            entity_indices.add(entity['index'])

    named_entities_that_are_not_fb_entities = []
    for i, word in enumerate(sent['words']):
        if word['pos'] == "NNP" and i not in entity_indices \
                and word['word'] != "_blank_":
            word['pos'] = "NN"
            named_entities_that_are_not_fb_entities.append(word['word'])

    if "synPars" in sent:
        for synpar in sent['synPars']:
            synpar_str = synpar['synPar']
            for word in named_entities_that_are_not_fb_entities:
                synpar_str = re.sub(" %s %s NNP" % (word, word), " %s %s NN" %
                                    (word, word), synpar_str, 1)
            synpar['synPar'] = synpar_str
    print json.dumps(sent)
