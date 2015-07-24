'''
This program was found not so useful. Freebase has changed many mid to id mappings which are not consistent with previous versions. An unfortunate change without thinking the consequences.

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


bad_ids = set()
for line in sys.stdin:
    line = json.loads(line)
    if "disambiguatedEntities" in line:
        for entities in line['disambiguatedEntities']:
            for entity in entities['entities']:
                id = entity['id'].split("/")[-1]
                if id in id_to_mid:
                    mid = id_to_mid[id]
                    if mid != entity['entity']:
                        sys.stderr.write(
                            id + ' ' + mid + ' ' + entity['entity'] + '\n')
                        bad_ids.add(id)
                    #entity['entity'] = mid
                else:
                    sys.stderr.write(id + 'not in mapping \n')
    # print json.dumps(line)

for id in bad_ids:
    print id