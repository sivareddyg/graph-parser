import sys
import json
import random
import os

items = []
for line in sys.stdin:
    sent = json.loads(line)
    items.append((sent['qid'], sent['sentence']))

random.seed(1)
random.shuffle(items)
random.shuffle(items)
random.shuffle(items)
random.shuffle(items)

no_of_splits = int(sys.argv[1])
splits = [[] for i in range(no_of_splits)]
for i, item in enumerate(items):
    splits[i % no_of_splits].append(item)

random.shuffle(splits)
out_folder = sys.argv[2]
if not os.path.exists(out_folder):
    os.mkdir(out_folder)

for i, split in enumerate(splits):
    f = open("%s/%s.csv" %(out_folder, i), "w")
    for item in split:
        f.write("%s\t%s\n" %(item[0], item[1].encode('utf-8', 'ignore')))
    f.close()
