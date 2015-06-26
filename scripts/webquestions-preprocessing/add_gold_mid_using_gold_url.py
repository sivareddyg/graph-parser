'''
Created on 26 Jun 2015

@author: siva
'''


import json
import os
import sys

id_to_mid = {}
for line in os.popen("zcat %s" % (sys.argv[1])):
    mid, name_id = line.strip().split("\t")
    id_to_mid[name_id] = mid

for line in sys.stdin:
    line = json.loads(line)
    if "url" in line:
        url = line['url'].split("/")[-1]
        mid = id_to_mid[url]
        line['goldMid'] = mid
    print json.dumps(line)
