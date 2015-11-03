import json
import sys

word = {"word": "?", "lemma": "?", "pos": ".", "ner":"O", "sentEnd" : True}
for line in sys.stdin:
    sent = json.loads(line)
    del sent['words'][-1]['sentEnd']
    sent['words'].append(word)
    print json.dumps(sent)
