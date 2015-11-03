#!/usr/bin/python

"""return a tuple with recall, precision, and f1 for one example"""
import json
import sys


def computeF1(goldList, predictedList):
    """Assume all questions have at least one answer"""
    if len(goldList) == 0:
        if len(predictedList) == 0:
            return (1, 1, 1)
        else:
            return (0, 0, 0)
        # raise Exception("gold list may not be empty")
    """If we return an empty list recall is zero and precision is one"""
    if len(predictedList) == 0:
        return (0, 1, 0)
    """It is guaranteed now that both lists are not empty"""

    precision = 0
    for entity in predictedList:
        if entity in goldList:
            precision += 1
    precision = float(precision) / len(predictedList)

    recall = 0
    for entity in goldList:
        if entity in predictedList:
            recall += 1
    recall = float(recall) / len(goldList)

    f1 = 0
    if precision + recall > 0:
        f1 = 2 * recall * precision / (precision + recall)
    return (recall, precision, f1)


def getResults(file):
    averageRecall = 0
    averagePrecision = 0
    averageF1 = 0
    count = 0

    """Go over all lines and compute recall, precision and F1"""
    with open(file) as f:
        for line in f:
            tokens = line.split("\t")
            gold = json.loads(tokens[1])
            predicted = json.loads(tokens[2])
            recall, precision, f1 = computeF1(gold, predicted)
            averageRecall += recall
            averagePrecision += precision
            averageF1 += f1
            count += 1

    """Print final results"""
    averageRecall = float(averageRecall) / count
    averagePrecision = float(averagePrecision) / count
    averageF1 = float(averageF1) / count
    returnString = ""
    returnString += "Number of questions: " + str(count)
    returnString += "\n"
    returnString += "Average recall over questions: " + str(averageRecall)
    returnString += "\n"
    returnString += "Average precision over questions: " + \
        str(averagePrecision)
    returnString += "\n"
    returnString += "Average f1 over questions (accuracy): " + str(averageF1)
    returnString += "\n"
    averageNewF1 = 2 * averageRecall * averagePrecision / \
        (averagePrecision + averageRecall)
    returnString += "F1 of average recall and average precision: " + \
        str(averageNewF1)
    return returnString
