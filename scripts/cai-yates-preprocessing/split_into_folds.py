'''
Program to split a data into several folds. The data is read from standard input.

arg1 should be the number of folds.
arg2 should be the target folder

Created on 20 Sep 2013

@author: Siva Reddy
'''

import json
import os
import random
import sys

random.seed(1)
splits = int(sys.argv[1])
target_folder = sys.argv[2]

if not os.path.exists(target_folder):
    os.mkdir(target_folder)

sentences = []
for line in sys.stdin:
    sentences.append(line)

# Shuffle 5 times.
random.shuffle(sentences)
random.shuffle(sentences)
random.shuffle(sentences)
random.shuffle(sentences)
random.shuffle(sentences)

split_size = len(sentences) / 10

# Each split will have the same number of sentences.
for i in range(splits):
    split_file = open(os.path.join(target_folder, str(i) + ".txt"), "w")
    for line in sentences[i * split_size: (i + 1) * split_size]:
        split_file.write(line)
    split_file.close()

# Remaining sentences. Each sentence goes to a split.
for i, line in enumerate(sentences[splits * split_size:]):
    split_file = open(os.path.join(target_folder, str(i) + ".txt"), "a")
    split_file.write(line)
    split_file.close()
