#!/usr/bin/env python

# This is the script used for evaluation by Berant et al. 
# (http://nlp.stanford.edu/software/sempre/).

# It has been extended to also provide accuracy in addition to
# average F1 and can handle two different date formats (see below).
# The different date formats seem to be an artifact of
# Freebase version + Virtuoso.

import sys
import json
import re

# Matches date formatted like this: 2/23/1836
DATE_MDY_RE = re.compile(r'(\d+)/(\d+)/(\d+)')
# Matches date formatted like this: 1836-02-23
DATE_YMD_RE = re.compile(r'(\d+)-(\d+)-(\d+)')

if len(sys.argv) != 2:
  sys.exit("Usage: %s <result_file>" % sys.argv[0])

def parse_date(date_string):
  """Try to parse string to date tuple."""
  year, month, day = None, None, None
  m = re.match(DATE_MDY_RE, date_string)
  if m:
    year = m.group(3)
    month = m.group(1)
    day = m.group(2)
  else:
    m = re.match(DATE_YMD_RE, date_string)
    if m:
      year = m.group(1)
      month = m.group(2)
      day = m.group(3)
  if year is not None:
    return int(year), int(month), int(day)
  else:
    return None

def parse_result_list(rlist):
  """Try to parse values to dates where possible."""
  result_list = []
  for e in rlist:
    date = parse_date(e)
    if date is not None:
      result_list.append(date)
    else:
      result_list.append(e)
  return result_list

"""return a tuple with recall, precision, and f1 for one example"""
def computeF1(goldList,predictedList):

  """Assume all questions have at least one answer"""
  if len(goldList)==0:
    if len(predictedList)==0:
        return (1,1,1)
    else:
        return (0,0,0)
  """If we return an empty list recall is zero and precision is one"""
  if len(predictedList)==0:
    return (0,1,0)
  """It is guaranteed now that both lists are not empty"""
  goldList = parse_result_list(goldList)
  predictedList = parse_result_list(predictedList)

  precision = 0
  for entity in predictedList:
    if entity in goldList:
      precision+=1
  precision = float(precision) / len(predictedList)

  recall=0
  for entity in goldList:
    if entity in predictedList:
      recall+=1
  recall = float(recall) / len(goldList)

  f1 = 0
  if precision+recall>0:
    f1 = 2*recall*precision / (precision + recall)
  return (recall,precision,f1)

averageRecall=0
averagePrecision=0
averageF1=0
nCorrect=0
count=0

"""Go over all lines and compute recall, precision and F1"""
with open(sys.argv[1]) as f:
  for line in f:
    tokens = line.split("\t")
    gold = json.loads(tokens[1])
    predicted = json.loads(tokens[2])
    recall, precision, f1 = computeF1(gold,predicted)
    if f1 == 1:
      nCorrect += 1
    averageRecall += recall
    averagePrecision += precision
    averageF1 += f1
    count+=1

"""Print final results"""
averageRecall = float(averageRecall) / count
averagePrecision = float(averagePrecision) / count
averageF1 = float(averageF1) / count
accuracy = float(nCorrect) / count
print "Number of questions: " + str(count)
print "Average recall over questions: " + str(averageRecall)
print "Average precision over questions: " + str(averagePrecision)
print "Average f1 over questions: " + str(averageF1)
print "Accuracy over questions: " + str(accuracy)
averageNewF1 = 2 * averageRecall * averagePrecision / (averagePrecision + averageRecall)
print "F1 of average recall and average precision: " + str(averageNewF1)

