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

domains = ["business", "film", "people"]
domain_sizes = [46, 49, 29]

positives_all = 0
negatives_all = 0
total_all = 0

size = 0
for j in range(len(domain_sizes)):
    i = domain_sizes[j]
    domain_results = results[size:size + i]
    positives = 0.0
    negatives = 0.0
    total = i
    
    for result in domain_results:
        if result == "1":
            positives += 1
        elif result == "0":
            negatives += 1
    prec = positives / (positives + negatives) * 100
    recall = positives / total * 100
    fmeas = 2 * prec * recall / (prec + recall) 
    print "%s\tpositives:%d negatives:%d total:%d Prec:%.2f Rec:%.2f Fmeas:%.2f" %(domains[j], positives, negatives, total, prec , recall, fmeas)
    positives_all += positives
    negatives_all += negatives
    total_all += total
    size = size + i

prec = positives_all / (positives_all + negatives_all) * 100
recall = positives_all / total_all * 100
fmeas = 2 * prec * recall / (prec + recall)
print "Combined\tpositives:%d negatives:%d total:%d Prec:%.2f Rec:%.2f Fmeas:%.2f" %(positives_all, negatives_all, total_all, prec , recall, fmeas)