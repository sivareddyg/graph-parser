'''
Created on 29 Aug 2013

@author: Siva Reddy
'''

import re
import sys

type_pattern = re.compile("((ns)|(rdf)):type([\.\s]|$)")
common_pattern = "ns:common.topic.alias"

for line in sys.stdin:
    if line[0] == "@":
        continue
    parts = line.split('\t')
    if len(parts) == 3:
        if type_pattern.match(parts[1]) or parts[1] == common_pattern:
            sys.stdout.write(line)
