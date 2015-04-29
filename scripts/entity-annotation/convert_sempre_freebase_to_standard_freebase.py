'''
Created on 26 Apr 2015

<http://rdf.freebase.com/ns/en.can_somebody_tell_me_who_i_am> => <http://rdf.freebase.com/ns/m.015cc1p>

@author: siva
'''

import os
import re
import sys

entity_lexicon = {}
entity_pattern = re.compile("<http://rdf\.freebase.com/ns/en\.(.*?)>")

for line in os.popen("zcat %s" % (sys.argv[1])):
    line = line.strip()
    parts = line.split("\t")
    entity_lexicon[parts[1]] = parts[0]

for line in sys.stdin:
    entities = entity_pattern.findall(line)
    for entity in entities:
        if entity in entity_lexicon:
            line = line.replace("en.%s>" % entity, "%s>" %
                                entity_lexicon[entity], 1)
    sys.stdout.write(line)
