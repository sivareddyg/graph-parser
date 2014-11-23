'''
Created on 20 Sep 2013

@author: Siva Reddy
'''

import sys
import json

# Output: {"entities": [{"index": 4, "score": 0.840163, "entity": "m.0gsg7"}, {"index": 13, "score": 0.997802, "entity": "m.011zfk"}, {"index": 14, "score": 0.976942, "entity": "m.019rl6"}], 
# "words": [{'word':"Among"}, {'word':"the"}, {'word':"most"}, {'word':"famous"}, {'word':"ABC"}, {'word':"are"}, {'word':"tennis"}, {'word':"player"}, {'word':"Michael"}, {'word':"Chen"}, {'word':"and"}, {'word':"Yahoo"}, {'word':"co-founder"}, {'word':"Jerry_Yang"}}

while 1:
    line = sys.stdin.readline()
    if line == "":
        break
    line = line.strip()
    if line != "":
        words = line.split()
        logical_form = sys.stdin.readline().strip()
        words = [{"word":word} for word in words]
        sent = {'words':words, 'originalLogicalForm':logical_form}
        print json.dumps(sent)