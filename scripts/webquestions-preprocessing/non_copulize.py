'''
Created on 26 May 2014

@author: siva
'''

import json
import re
import sys

for line in sys.stdin:
    line = json.loads(line)
    # print line
    sentence = line['sentence']
    sentence_mod = " ".join([word["word"] for word in line["words"]])
    if re.match("((What)|(Who)) ((is)|(was)|(were)) [^\s]+ \?", sentence_mod):
        words = line["words"]
        words[0] = {"word" : "What", "ner" : "0"}
        words[1] = {"word" : "does", "ner" : "0"}
        words[3] = {"word" : "do", "ner" : "0"}
        words.append({"word" : "?", "ner" : "0"})
    print json.dumps(line)