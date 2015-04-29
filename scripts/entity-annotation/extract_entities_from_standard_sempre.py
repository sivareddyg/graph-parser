'''
Created on 27 Apr 2015

<http://rdf.freebase.com/ns/m.0jb988j>  <http://rdf.freebase.com/ns/american_football.football_historical_roster_position.number>       "1"^^xsd:int .

=> m.0jb988j

@author: siva
'''

import re
import sys

entity_pattern = re.compile("<http://rdf\.freebase.com/ns/(m\..*?)>")
entities_all = set()

for line in sys.stdin:
    entities = entity_pattern.findall(line)
    for entity in entities:
        if entity not in entities_all:
            entities_all.add(entity)
            print entity