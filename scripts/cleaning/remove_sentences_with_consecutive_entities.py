'''
Created on 26 May 2015

@author: siva
'''

import json
import re
import sys


for line in sys.stdin:
    try:
        line = json.loads(line)
        sentence = " ".join(word['word'] for word in line['words'])
	sentence = sentence.strip(". ")
        if re.search("[^\s]+ , [^\s]+ , ", sentence):
            sys.stderr.write(sentence + "\n")
            continue
        if re.search("[^\s]+ , the [^\s]+ , ", sentence):
            sys.stderr.write(sentence + "\n")
            continue
        if re.search("[^\s]+ , [^\s]+ and ", sentence):
            sys.stderr.write(sentence + "\n")
            continue
        if re.search("[^\s]+ , the [^\s]+ and ", sentence):
            sys.stderr.write(sentence + "\n")
            continue
        print json.dumps(line)
    except ValueError:
       sys.stderr.write(line); 
