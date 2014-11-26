import sys
from unitok import LANGUAGE_DATA, tokenise
import re
import json


lsd = LANGUAGE_DATA['english']()

while 1:
    line = sys.stdin.readline()
    if line == "":
        break
    line = line.strip()
    if line != "":
        sentence = json.loads(line)
        sent = sentence['utterance']
        sent = sent.strip("?")
        if re.match("how[\s]+many[\s]", sent):
            sent = re.sub("how[\s]+many[\s]", "how-many ", sent);
        if re.match("how[\s]+much[\s]", sent):
            sent = re.sub("how[\s]+much[\s]", "how-much ", sent);
        #if re.match("when[\s]", sent) and not re.match("when[\s]+is[\s]", sent):
        #    sent = re.sub("when[\s]", "what year ", sent);
        #if re.match("when[\s]+is[\s]", sent):
        #    sent = re.sub("when[\s]+is[\s]", "what is ", sent);
        #if re.match("where[\s]+was[\s]", sent):
        #    sent = re.sub("where[\s]+was[\s]", "in what location was ", sent);
        #if re.match("where .* to [^\s]+$", sent):
        #    sent = re.sub("where (.* to) ([^\s]+)$", "what \\2 \\1", sent)
        #if re.match("where[\s]+", sent):
            # sent = re.sub("where[\s]+", "what location ", sent)
        #    if not re.search(" ((at)|(from)|(in)|(to)|(of))$", sent):
        #        sent = sent + " at" 
        if re.match("how[\s]+did[\s]", sent):
            sent = re.sub("how[\s]+did[\s]", "what did ", sent)
            sent = sent + " of"
        # if  re.match("what year", sent) and not re.match("what year .* in[\s]*$", sent):
        #    sent = sent + " in "
        
        sent = re.sub(" ((the)|(a)) ((name[s]?)|(kind[s]?)|(type[s]?)) of", "", sent)
        sent = re.sub(" ((name[s]?)|(kind[s]?)|(type[s]?)) of", "", sent)
        sent = re.sub(" ((name[s]?)|(kind[s]?)|(type[s]?))", "", sent)
        
        words = line.split()
        # print json.dumps(sent)
        tokens = tokenise(sent, lsd, None)
        if len(tokens) > 0:
            tokens[0] = tokens[0][0].upper() + tokens[0][1:]
        words = []
        for word in tokens:
            words.append({"word":word})
        words.append({'word': '?'})
        sentence['sentence'] = sentence['utterance']
        del sentence['utterance']
        sentence['words'] = words
        print json.dumps(sentence)
