'''
Created on 25 Apr 2015

@author: siva
'''

import re
import sys


lang_codes = set(sys.argv[1].strip().split(","))
predicates = re.compile("type.object.key|type.object.name|common.topic.alias")
for line in sys.stdin:
    if predicates.search(line):
        line = line.rstrip(" .\n")
        entity, relation, name = line.split("\t", 2)
        if not entity.startswith("ns:m."):
            continue
        if "en" in lang_codes and relation == "ns:type.object.key":
            if name.startswith('"/en/'):
                name = " ".join(name[5:-1].split("_"))
                # print relation
                print "%s\t%s" % (entity[3:], name)
        else:
            breakpoint = name.rfind("@")
            lang = name[breakpoint + 1:]
            name = name[:breakpoint]
            if lang in lang_codes:
                name = name.strip('"')
                # print relation
                print "%s\t%s" % (entity[3:], name)