'''
Created on 11 Oct 2016

@author: siva
'''

import json
import sys
import os
import re

questions = json.loads(sys.stdin.read())

id_to_mid = {}
for line in os.popen("zcat %s" % (sys.argv[1])):
    mid, name_id = line.strip().split("\t")
    id_to_mid[name_id] = mid

for question in questions:
    question["sentence"] = question["question"]
    question['goldMids'] = []
    question['id'] = question['qid']
    del question['question']
    for node in question['graph_query']['nodes']:
        # print node
        if node['node_type'] == 'entity':
            entity_id = re.sub("^en\.", "", node['id'])
            if entity_id in id_to_mid:
                question['goldMids'].append(id_to_mid[entity_id])
    print json.dumps(question)
    