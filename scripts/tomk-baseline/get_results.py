'''
Created on 27 Dec 2013

@author: Siva Reddy
'''

import sys
import re
import json


def evaluate(sentences, out_predictions):
    domain_positives = 0
    domain_negatives = 0
    domain_total = len(sentences)
    for line in sentences:
        line = line.strip()
        if out_predictions[line] == "CORRECT":
            domain_positives += 1
        elif out_predictions[line] == "WRONG":
            domain_negatives += 1
    precision = (domain_positives + 0.0)/(domain_positives + domain_negatives) * 100
    recall = (domain_positives + 0.0)/(domain_total) * 100
    fmeasure = 2.0 * precision * recall / (precision + recall)
    print "DOMAIN POSITIVES:%d NEGATIVES:%d TOTAL:%d PREC:%.1f REC:%.1f FMEAS:%.1f" %(domain_positives, domain_negatives, domain_total, precision, recall, fmeasure)        


domain_file = sys.argv[1]
domain_sentences = []
for line in open(domain_file):
    parts = json.loads(line)
    sentence = parts["sentence"]
    sentence = sentence.replace("charlie_s", "charlie 's")
    sentence = sentence.replace("russell", "russel")
    sentence = sentence.replace("gamble's", "gamble 's")
    domain_sentences.append(sentence)

testing_block = True
iteration_number = 0
positives = 0
negatives = 0
total = 0
predictions = {}
is_predicted = False
for line in sys.stdin:
    line = line.strip()
    if re.match("TESTING:", line):
        testing_block = True
        print "ITERATION %d" %(iteration_number)
        predictions = {}
        positives = 0
        negatives = 0
        total = 0
        iteration_number += 1
    elif re.match("Training ", line):
        if (positives + negatives > 0):
            precision = (positives + 0.0)/(positives + negatives) * 100
            recall = (positives + 0.0)/total * 100
            fmeasure = 2.0 * precision * recall / (precision + recall)
            print "POSITIVES:%d NEGATIVES:%d TOTAL:%d PREC:%.1f RECALL:%.1f FMEAS:%.1f" %(positives, negatives, total, precision, recall, fmeasure)
            evaluate(domain_sentences, predictions)
            print "##########################################"
            print
        testing_block = False
        
    elif testing_block and re.match("Q: ", line):
        question = re.search("Q: (.*)", line).group(1)
        is_predicted = True
    elif testing_block and re.match("0 underspecified parses", line):
        is_predicted = False
    elif testing_block and re.match("[0-9]+ correct", line):
        current_positives = int(re.search("([0-9]+) correct", line).group(1))
        total += 1
        if (current_positives == positives):
            if is_predicted:
                predictions[question] = "WRONG"
                print "%s\tWRONG" %(question) 
                negatives += 1
            else:
                predictions[question] = "UNKNOWN"
                print "%s\tUNKNOWN" %(question)
        else:
            predictions[question] = "CORRECT"
            print "%s\tCORRECT" %(question)
            positives += 1

if (positives + negatives > 0):
    precision = (positives + 0.0)/(positives + negatives) * 100
    recall = (positives + 0.0)/total * 100
    fmeasure = 2.0 * precision * recall / (precision + recall)
    print "POSITIVES:%d NEGATIVES:%d TOTAL:%d PREC:%.1f RECALL:%.1f FMEAS:%.1f" %(positives, negatives, total, precision, recall, fmeasure)
    evaluate(domain_sentences, predictions)