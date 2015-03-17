'''
Created on 8 Mar 2015

@author: siva
'''

import os
import string
import sys

entities_to_name = {}
for line in open(sys.argv[1]):
    # george wilson :- NP : m.0gmk9p
    line = line.strip()
    (name, entity) = line.split(" :- NP : ")
    names = entities_to_name.setdefault(entity, set())
    names.add(name)

for line in os.popen("zcat %s" % (sys.argv[2])):
    # m.01_6xcx "Connie's Pizza , 2373 S. Archer Ave. , Chicago , IL"@en
    (entity, name) = line.split(" ", 1)
    if entity not in entities_to_name:
        continue
    name = name[0:name.rfind("@")]
    name = name.strip('"')
    name = name.lower()
    words = name.split()
    punctuation_stripped = []
    for word in words:
        if word in string.punctuation:  # do not use punctuation.
            continue
        punctuation_stripped.append(word)
    name_no_punc = " ".join(punctuation_stripped)
    entities_to_name[entity].add(name)
    entities_to_name[entity].add(name_no_punc)

for entity in entities_to_name:
    # george wilson :- NP : m.0gmk9p
    for name in entities_to_name[entity]:
        print "%s :- NP : %s" % (name, entity)
