'''
Created on 26 Feb 2014

@author: siva
'''

import commands
import sys

out_file = sys.argv[1]
results_line = commands.getoutput("tail -n1 %s" % (out_file))

results_line = results_line.split(" INFO : ")[1].strip()

results = results_line.split()

for result in results:
    if result == "1":
        print "1 1 1"
    elif result == "0":
        print "0 1 1"
    else:
        print "0 0 1"