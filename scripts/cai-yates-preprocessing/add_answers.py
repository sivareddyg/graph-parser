'''
Created on 9 Nov 2015

@author: siva
'''
import json
import sys

answers = {}
for line in open(sys.argv[1]):
    parts = line.strip().split("\t")
    answers[parts[0]] = json.loads(parts[1])

for line in sys.stdin:
    sent = json.loads(line)
    # (list (description \"Jamaican Creole English Language\") (description \"Jamaican English\"))"
    descriptions = []
    for answer in answers[sent['sentence']]:
        descriptions.append('(description "%s")' % (answer))
    target_value = "(list " + " ".join(descriptions) + ")"
    sent['targetValue'] = target_value
    print json.dumps(sent)