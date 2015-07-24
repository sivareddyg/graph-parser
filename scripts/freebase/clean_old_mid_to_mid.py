'''
Created on 20 Jul 2015

@author: siva
'''

import sys

# <http://rdf.freebase.com/ns/m.0102bryv>
# <http://rdf.freebase.com/ns/m.01081kyj> -> m.0102bryv m.01081kyj

# ns:m.010fp53    ns:m.01b1mhz -> m.010fp53   m.01b1mhz

for line in sys.stdin:
    line = line.replace("<http://rdf.freebase.com/ns/", "")
    line = line.replace(">", "")
    line = line.replace("ns:", "")
    line = line.strip()
    line = line.strip(".")
    print line
