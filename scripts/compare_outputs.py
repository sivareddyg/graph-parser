import sys
import json


model1 = "0 1 0 1 1 1 0 1 1 1 1 1 1 1 0 1 0 1 1 0 1 1 1 1 0 1 0 1 0 0 1 0 1 0 0 1 1 0 0 0 1 0 0 1 1 0 0 1 0 0 0 1 0 0 1 0 0 0 1 0 0 1 0 0 1 0 0 1 1 1 1 1 1 0 1 1 0 0 1 1 1 0 0 1 1 0 0 1 0 1 0 1 1 0 0 1 0 0 1 0 0 1 1 0 1 1 1 1 1 0 0 1 0 1 1 1 0 1 1 0 1 1 1 1 1 0 0 0 1 1 1 1 1 1 0 1 1 0 1 1 1 1 0 0 0 0 0 1 1 0 1 0 0 1 1 0 1 1 1 1 1 0 0 0 1 0 1 0 0 0 1 1 0 0 1 1 1 1 0 0 0 0 1 0 1 1 1 0 0 1 1 0 0 1 1 0 1 0 0 1"

model1 = [int(x) for x in model1.split()]

model2 = "0 -1 0 1 -1 1 0 1 1 -1 1 1 -1 1 0 0 1 1 1 -1 -1 1 1 0 0 1 0 1 0 0 0 0 -1 0 0 1 1 -1 0 0 0 0 0 -1 -1 0 0 1 0 0 0 -1 -1 0 1 0 0 0 -1 0 0 1 0 1 1 0 0 1 1 1 -1 1 1 0 1 1 0 0 1 1 1 0 0 -1 1 0 0 0 0 -1 0 0 1 0 1 1 0 1 -1 0 0 1 0 0 1 0 1 -1 1 1 0 1 0 -1 1 0 0 -1 1 0 -1 1 -1 1 -1 0 0 0 -1 1 0 1 1 0 0 -1 1 0 1 -1 1 -1 0 1 0 -1 -1 1 1 0 1 0 0 0 0 0 1 -1 0 1 1 0 0 0 1 0 1 0 0 0 -1 -1 -1 0 1 1 1 1 -1 0 -1 0 1 0 0 -1 1 0 -1 1 1 0 0 1 0 0 -1 0 0 1"

model2 = [int(x) for x in model2.split()]

sents = [];
for sent in sys.stdin:
	sent = json.loads(sent)
	words = [word['word'] for word in sent['words']]
	sents.append(" ".join(words))


for i, sent in enumerate(sents):
	if model1[i] == 1 and model2[i] == 0:
		print "%d\twrong grounded graph\t%s" %(i, sent)
	elif model1[i] == 1 and model2[i] == -1:
		print "%d\tno ungrounded graph\t%s" %(i, sent)
	elif model1[i] != -1 and model2[i] == -1:
		print "%d\tno ungrounded graph\t%s" %(i, sent)
