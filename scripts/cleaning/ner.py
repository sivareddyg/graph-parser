'''
Created on 5 Sep 2013

@author: Siva Reddy
'''

import random
import sys
import json
import os

def process_ner_file(orig_file, ner_file):
    orig = open(orig_file)
    ner = open(ner_file)
    orig_line = orig.readline()
    ner_line = ner.readline()
    while orig_line != "" and ner_line != "":
        ner_line = ner_line[:-1]
        sent = json.loads(orig_line[:-1])
        ner_words = ner_line.split()[1:]
        if len(ner_words) != len(sent['words']):
            orig_line = orig.readline()
            ner_line = ner.readline()
            continue
            # sys.stderr.write("ERROR: NER and input words size differ\n")
            # sys.stderr.write(json.dumps(ner_words) + '\n')
            # sys.stderr.write(json.dumps(sent['words']) + '\n')
            # exit(1)
        for i, ner_word in enumerate(ner_words):
            sent['words'][i]['ner'] = ner_word.split("/")[-1]
        orig_line = orig.readline()
        ner_line = ner.readline()
        prev_entity = "0"
        new_words = []
        old_to_new = {}
        j = 0
        new_word = ""
        i = 0
        words = sent['words']
        while i < len(words):
            # print words[i]['ner']
            if words[i]['ner'] == 'O':
                new_words.append(words[i])
                old_to_new[i] = j
                i += 1
                j += 1
            else:
                prev_entity = words[i]['ner']
                cur_entity = words[i]['ner']
                new_word = []
                while cur_entity == prev_entity:
                    old_to_new[i] = j
                    new_word.append(words[i]['word'])
                    i += 1
                    if i >= len(words):
                        break
                    prev_entity = cur_entity
                    cur_entity = words[i]['ner']
                new_words.append({'word' : "_".join(new_word), 'ner' : prev_entity})
                j += 1
                
        for entity in sent['entities']:
            entity['index'] = old_to_new[entity['index']]
        sent['words'] = new_words
        print sent
    os.system("rm -rf %s %s" %(orig_file, ner_file))

random.seed()
file_name = str(random.randint(0, 10**9))

orig_filename = "/tmp/orig_" + file_name
orig = open(orig_filename, 'w')

ner_inp_filename = "/tmp/ner_inp_" + file_name
ner_inp = open(ner_inp_filename, 'w')

ner_out_filename = "/tmp/ner_out_" + file_name

sys.stderr.write("temp file is %s\n" %(ner_out_filename))

i = 0
nercount = 50000

NER = "sh ../../software/stanford-ner-2012-11-11/ner.sh"
for line in sys.stdin:
    orig.write(line)
    line = line[:-1]
    sent = json.loads(line)
    words = [word['word'] for word in sent['words']]
    ner_inp.write("dummy " + " ".join(words).encode('utf-8', 'ignore') + '\n')
    i += 1
    if i % nercount == 0:
        sys.stderr.write("NER Processed: %d\n" %(i))
        ner_inp.close()
        orig.close()
        command = "%s %s > %s" %(NER, ner_inp_filename, ner_out_filename)
        sys.stderr.write(command + '\n')
        os.system(command)
        process_ner_file(orig_filename, ner_out_filename)
        ner_inp = open(ner_inp_filename, 'w')
        orig = open(orig_filename, 'w')

sys.stderr.write("NER Processed: %d\n" %(i))
ner_inp.close()
orig.close()
command = "%s %s > %s" %(NER, ner_inp_filename, ner_out_filename)
sys.stderr.write(command + '\n')
os.system(command)
process_ner_file(orig_filename, ner_out_filename)