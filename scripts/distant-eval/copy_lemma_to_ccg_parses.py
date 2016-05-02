'''
Created on 30 Apr 2016

@author: siva
'''

import json
import sys

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    sent = json.loads(line)
    lemmas = [word['lemma'] for word in sent['words']]
    if 'synPars' in sent:
        for synParObj in sent['synPars']:
            synPar = synParObj['synPar']
            leaves = synPar.findall("(<L [^\>]+>)")
            print leaves
    # print json.dumps(sent)
