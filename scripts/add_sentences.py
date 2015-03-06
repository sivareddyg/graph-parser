'''
Created on Nov 22, 2014

@author: sivareddy
'''

import sys
import json
import re

if __name__ == "__main__":
  for i, line in enumerate(sys.stdin):
    document = json.loads(line.strip())
    if 'sentence' not in document:
      sentence = " ".join([word['word'] for word in document['words']])
      document['sentence'] = sentence
    print json.dumps(document)
