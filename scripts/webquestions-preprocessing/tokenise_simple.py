import sys
from unitok import LANGUAGE_DATA, tokenise
import re
import json


lsd = LANGUAGE_DATA['english']()

while 1:
    line = sys.stdin.readline()
    if line == "":
        break
    line = line.strip()
    if line != "":
        sentence = json.loads(line)
        sent = sentence['utterance']
        sent = sent.strip("?")
        
        words = line.split()
        # print json.dumps(sent)
        tokens = tokenise(sent, lsd, None)
        if len(tokens) > 0:
            tokens[0] = tokens[0][0].upper() + tokens[0][1:]
        words = []
        for word in tokens:
            words.append({"word":word})
        words.append({'word': '?'})
        sentence['sentence'] = sentence['utterance']
        del sentence['utterance']
        sentence['words'] = words
        print json.dumps(sentence)
