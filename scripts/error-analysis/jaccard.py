import sys
import json

totalJaccard = 0.0
totalLineCount = 0.0
for line in sys.stdin:
	sent, gold, pred = line.strip().split("\t")
	gold = set(json.loads(gold))
	pred = set(json.loads(pred))
	jaccard = len(gold.intersection(pred))/len(gold.union(pred)) if len(gold) > 0 else 0
	totalJaccard += jaccard
	totalLineCount += 1.0
print totalJaccard/totalLineCount
