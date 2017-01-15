import json
import sys
from sempre_evaluation_lib import computeF1 

total = 0.0
total_f1 = 0.0
for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    forest = json.loads(line)
    gold = forest['answer'] if 'answer'in forest else forest['answerF1']
    f1 = 0.0
    for sent in forest['forest']:
        for graph in sent['graphs']:
            f1 = max(f1, computeF1(gold, graph['denotation'])[2])
    total_f1 += f1
    total += 1.0

print total_f1 * 100.0 / total
