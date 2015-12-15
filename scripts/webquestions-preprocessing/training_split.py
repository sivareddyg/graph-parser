'''
Created on 26 May 3014

@author: siva
'''

import sys
import json
import random

if len(sys.argv) > 2:
    dev_split_size = int(sys.argv[2])
else:
    dev_split_size = 30

training_file = open(sys.argv[1] + ".%d" %(100 - dev_split_size), "w")
dev_file = open(sys.argv[1] + ".%d" %(dev_split_size), "w")

sys.stderr.write("Creating training and dev splits\n");

random.seed(1)

data = open(sys.argv[1]).readlines()
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)

training_data_size = (100 - dev_split_size) * len(data) / 100

for line in data[:training_data_size]:
    training_file.write(line)
training_file.close()

for line in data[training_data_size:]:
    dev_file.write(line)
dev_file.close()
