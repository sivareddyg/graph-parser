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

sparql_to_sent = {}
for line in open(sys.argv[1]):
    sent = json.loads(line)
    query = sent['sparql_query']
    if query not in sparql_to_sent:
        sparql_to_sent[query] = []
    sparql_to_sent[query].append(line)

data = sparql_to_sent.items()

random.seed(1)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)
random.shuffle(data)

training_data_size = (100 - dev_split_size) * len(data) / 100

training_data = []
for query in data[:training_data_size]:
    training_data += query[1]
random.shuffle(training_data)

dev_data = []
for query in data[training_data_size:]:
    dev_data += query[1]
random.shuffle(dev_data)

for sent in training_data:
    training_file.write(sent)
training_file.close()

for sent in dev_data:
    dev_file.write(sent)
dev_file.close()
