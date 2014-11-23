'''
Created on 26 May 2014

@author: siva
'''

import json
import re
import sys

for line in sys.stdin:
    line = json.loads(line)
    # print line
    # sentence = line['sentence']
    sentence = " ".join([word["word"] for word in line["words"]])
    
    if re.search(" do \?$", sentence):
        # what did Einstein do?
        # sentence = re.sub(" do\?$", " serve as\?", sentence)
        words = line['words']
        words.pop(-1)
        words.pop(-1)
        word = { "word" : "profession", "ner" : "0"}
        words.append(word)
        word = { "word" : "?", "ner" : "0"}
        words.append(word)
        
        for word in words:
            if word['word'] == 'did' or word['word'] == 'do' or word['word'] == 'does':
                 word['word'] = 'is'
        
    if re.search("Where ((is)|(was)) .* from \?$", sentence):
        # where is Obama from ?
        #sentence = re.sub(" from\?$", " born in ?", sentence)         
        words = line['words']
        entities = line['entities']
        check = False
        for entity in entities:
            if entity["index"] == len(words) - 3:
                check = True
        if check:
            words.pop(-1)
            words.pop(-1)
            word = { "word" : "born", "ner" : "0"}
            words.append(word)
            word = { "word" : "in", "ner" : "0"}
            words.append(word)
            word = { "word" : "?", "ner" : "0"}
            words.append(word)
    '''if re.search("((name)|(type)|(kind))", sentence):
        # What is the name of the president of US
        #sentence = re.sub(" the ((name[s]?)|(type[s]?)|(kind[s]?)) of", "", sentence)
        #sentence = re.sub(" ((name[s]?)|(type[s]?)|(kind[s]?)) of", "", sentence)
        #sentence = re.sub(" ((name[s]?)|(type[s]?)|(kind[s]?))", "", sentence)
        
        words = line['words']
        entities = line['entities']
        for i, word in enumerate(words):
            if re.match("((name)|(kind)|(type))", word['word']):
                if len(words) > i + 1 and words[i + 1]["word"] == "of":
                    words.pop(i)
                    words.pop(i)
                    for entity in entities:
                        if entity["index"] > i:
                            entity["index"] += -2
                else:
                    words.pop(i)
                
                if words[i - 1]["word"] == "the" or words[i - 1]["word"] == "a":
                    words.pop(i - 1)
                    for entity in entities:
                        if entity["index"] > i - 1:
                            entity["index"] += -1
                break'''
    
    sentence_mod = " ".join([word["word"] for word in line["words"]])
    # print sentence_mod
    if re.match("((What)|(Who)) ((is)|(was)) [^\s]+ \?", sentence_mod):
        words = line["words"]
        words[0] = {"word" : "What", "ner" : "0"}
        words[1] = {"word" : "is", "ner" : "0"}
        words[3] = {"word" : "'s", "ner" : "0"}
        words.append({"word" : "profession", "ner" : "0"})
        words.append({"word" : "?", "ner" : "0"})

    print json.dumps(line)