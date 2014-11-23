'''
Created on 22 Nov 2013

@author: Siva Reddy
'''

import hashlib
import sys
import json
import gzip

sentences_hashes = {} # sent_hash_key : [list of entities]
sent_file = sys.argv[1]

f = gzip.open(sent_file, "rb")
count = 0;
for line in f:
    count += 1;
    if count % 10000 == 0:
        sys.stderr.write(str(count) + '\n')
    line = line.rstrip()
    parts = json.loads(line)
    sent = parts[0]
    entities = parts[1:]
    sent = sent.encode('ascii', 'replace')
    # sys.stderr.write(sent + "\n")
    hashkey = hashlib.md5(sent).hexdigest()
    if sentences_hashes.has_key(hashkey):
        oldEntities = sentences_hashes[hashkey]
        newEntities = []
        for entityNew in entities:
            isValid = True
            for entityOld in sentences_hashes[hashkey]:
                if (entityNew[2] >= entityOld[2] and entityNew[2] <= entityOld[3]) or (entityNew[3] >= entityOld[2] and entityNew[3] <= entityOld[3]):
                    isValid = False
                    break
            if isValid:
                newEntities.append(entityNew)
        if newEntities != []:
            oldEntities.extend(newEntities)
    else:
        sentences_hashes[hashkey] = entities
f.close()


f = gzip.open(sent_file, "rb")
for line in f:
    line = line.rstrip()
    parts = json.loads(line)
    sent = parts[0]
    sent = sent.encode('ascii', 'replace')
    # sys.stderr.write(sent + "\n")
    hashkey = hashlib.md5(sent).hexdigest()
    if sentences_hashes.has_key(hashkey):
        entities = sentences_hashes.pop(hashkey)
        entities.sort(key = lambda x : x[2])
        sent = [sent, entities]
        print json.dumps(sent)