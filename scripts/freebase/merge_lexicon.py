'''
Created on 20 Feb 2014

@author: siva
'''

import sys
import logging

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s', level=logging.INFO)

is_type = False
is_relation = False

types_freq = {}
types = {}
relations_freq = {}
relations = {}

use_empty_types = False
if len(sys.argv) > 1:
    use_empty_types = True

for line in sys.stdin:
    line = line.rstrip()
    if line == "" or line[0] == "#":
        continue
    if line[0] != "\t":
        left, right = line.split("\t")
        if len(left.split()) > 1:
            # logging.info("%s %s %s", left, right, str(left.split()))
            is_relation = True
            is_type = False
            if left not in relations_freq:
                relations_freq[left] = 0.0
            relations_freq[left] += float(right)
            urelation = left
            if urelation not in relations:
                relations[urelation] = {}
        else:
            # logging.info("Yes")
            is_relation = False
            is_type = True
            if left not in types_freq:
                types_freq[left] = 0.0
            types_freq[left] += float(right)
            utype = left
            if utype not in types:
                types[utype] = {}
    else:
        left, right = line.strip().split("\t")
        if is_type:
            if left not in types[utype]:
                types[utype][left] = 0.0
            types[utype][left] +=  float(right)
        elif is_relation:
            if left not in relations[urelation]:
                relations[urelation][left] = 0.0
            relations[urelation][left] += float(right)
  
print "# Language Types to Grounded Types"          
for utype, freq in sorted(types_freq.items(), key = lambda x : x[1], reverse = True):
    print "%s\t%f" %(utype, freq)
    for gtype, freq in sorted(types[utype].items(), key = lambda x : x[1], reverse = True):
        print "\t%s\t%f" %(gtype, freq)
    
print "# Language Predicates to Grounded Relations"
for urel, freq in sorted(relations_freq.items(), key = lambda x : x[1], reverse = True):
    print "%s\t%f" %(urel, freq)
    for grel, freq in sorted(relations[urel].items(), key = lambda x : x[1], reverse = True):
        print "\t%s\t%f" %(grel, freq)

