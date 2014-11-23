'''
Created on 19 Dec 2013

@author: Siva Reddy
'''

import json
import sys
import re

for line in sys.stdin:
    line = line.strip()
    if line == "" or line[0] == '#':
        continue
    sentence = json.loads(line)
    if not sentence.has_key("synPars"):
        print line
        continue
    words = sentence["words"]
    word_sent = " ".join(word['word'].lower() for word in words)
    # print word_sent
    whenWordPosition = word_sent.find("in what year");
    if whenWordPosition >= 0:
        whenWordPosition += len("in what")
        yearPosition = len(word_sent[:whenWordPosition].split())
        words[yearPosition]["ner"] = 'DATE'
        for synPar in sentence["synPars"]:
            leaves = re.findall("(\(\<L ([^\>]+) [^\>]+ [^\>]+ [^\>]+ [^\>]+ [^\>]+ ([^\>]+)\>\))", synPar['synPar'])
            newReplacement = "(<L %s %s %s CD I-DAT I-NP %s>)" %(leaves[yearPosition][1], "year", "year", leaves[yearPosition][2])
            synPar['synPar'] = synPar['synPar'].replace(leaves[yearPosition][0], newReplacement)
    #print json.dumps(sentence)
    
    '''(<T S[wq] rp 0 2> (<T S[wq] fa 0 2> (<T S[wq]/(S[q]/PP) fa 0 2> 
    (<L (S[wq]/(S[q]/PP))/(S[wq]/(S[q]/NP)) In in IN O I-PP (S[wq]/(S[q]/PP))/(S[wq]/(S[q]/NP))>) (<T S[wq]/(S[q]/NP) fa 0 2> 
    (<L (S[wq]/(S[q]/NP))/N what what WDT O I-NP (S[wq]/(S[q]/NP))/N>) 
    (<L N year year NN O I-NP N>))) (<T S[q]/PP fc 0 2> (<T S[q]/(S[b]\\NP) fa 0 2> 
    (<L (S[q]/(S[b]\\NP))/NP did do VBD O I-VP (S[q]/(S[b]\\NP))/NP>) (<T NP lex 0 1> 
    (<L N Motorola Motorola NNP I-LOC I-NP N>)))'''
    
    for i, word in enumerate(words):
        if word.has_key("actual") and (word['actual'].lower() == 'how-many' or word['actual'].lower() == 'how-much'):
            for synPar in sentence["synPars"]:
                # print synPar['synPar']
                leaves = re.findall("(\(\<L ([^\>]+) [^\>]+ [^\>]+ [^\>]+ [^\>]+ [^\>]+ ([^\>]+)\>\))", synPar['synPar'])
                newReplacement = "(<L %s %s %s CD 0 I-NP %s>)" %(leaves[i][1], word['actual'], word['actual'].lower(), leaves[i][2])
                synPar['synPar'] = synPar['synPar'].replace(leaves[i][0], newReplacement)
                # print synPar['synPar']
                if word['actual'].lower() == 'how-much':
                    words[i]["ner"] = 'FLOAT'
                if word['actual'].lower() == 'how-many':
                    words[i]["ner"] = 'INT'
            word['word'] = word['actual']
            del word['actual']
    
    print json.dumps(sentence)