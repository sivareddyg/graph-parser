import sys
from unitok import LANGUAGE_DATA, tokenise
import re
import json

sempre_annotations_file = sys.argv[1]

annotated_sentences = {}

for line in open(sempre_annotations_file):
    line = line.decode('ascii', 'ignore')
    sentence = json.loads(line)
    annotated_sentences[sentence['utterance']] = sentence

lsd = LANGUAGE_DATA['english']()

while 1:
    line = sys.stdin.readline()
    if line == "":
        break
    line = line.strip()
    if line != "":
        line = line.decode('ascii', 'ignore')
        sentence = annotated_sentences[line]
        sent = line
        sent = sent.strip("?")
        if re.match("how[\s]+many[\s]", sent):
            sent = re.sub("how[\s]+many[\s]", "how-many ", sent);
        if re.match("how[\s]+much[\s]", sent):
            sent = re.sub("how[\s]+much[\s]", "how-much ", sent);
        if re.match("when[\s]", sent) and not re.match("when[\s]+is[\s]", sent):
            sent = re.sub("when[\s]", "what year ", sent);
        if re.match("when[\s]+is[\s]", sent):
            sent = re.sub("when[\s]+is[\s]", "what is ", sent);
        if re.match("where .* to [^\s]+$", sent):
            sent = re.sub("where (.* to) ([^\s]+)$", "what \\2 \\1", sent)
        if re.match("where[\s]+", sent):
            if not re.search(" ((at)|(from)|(in)|(to)|(of))$", sent):
                sent = sent + " at" 
        if re.match("how[\s]+did[\s]", sent):
            sent = re.sub("how[\s]+did[\s]", "what did ", sent)
            sent = sent + " of"
        if  re.match("what year", sent) and not re.match("what year .* in[\s]*$", sent):
            sent = sent + " in "
        
        sent = re.sub(" ((the)|(a)) ((name[s]?)|(kind[s]?)|(type[s]?)) of", "", sent)
        sent = re.sub(" ((name[s]?)|(kind[s]?)|(type[s]?)) of", "", sent)
        sent = re.sub(" ((name[s]?)|(kind[s]?)|(type[s]?))", "", sent)
        
        words = line.split()
        logical_form = sys.stdin.readline().strip()
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
        sentence['originalFormula'] = logical_form
        sentence['words'] = words
        # sent = {'words':words, 'originalLogicalForm':logical_form}
        print json.dumps(sentence)
