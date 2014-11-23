'''
Created on 26 Nov 2013

@author: Siva Reddy
'''

import sys
import json
import os
import re
# from subprocess import Popen, PIPE
import tempfile


def parse_sentences(model, first_word_lower=False, add_poss=True):
    pos_command = """./tools/candc_nbest/bin/pos --model %s """ % (model + "/pos")
    
    actual_line = " "
    while actual_line != "":
        count = 0
        actualFile = tempfile.NamedTemporaryFile(prefix="siva_candc_", dir="./working", delete=False);
        inputFile = tempfile.NamedTemporaryFile(prefix="siva_candc_", dir="./working", delete=False);
        outFile = tempfile.NamedTemporaryFile(prefix="siva_candc_", dir="./working", delete=False);
        
        sys.stderr.write("Temporary Actual file: %s\n" %(actualFile.name))
        sys.stderr.write("Temporary Input: %s\n" % (inputFile.name))
        sys.stderr.write("Temporary Output: %s\n" % (outFile.name))
        
        while actual_line != "":
            count += 1
            if count % 10000 == 0:
                break
            actual_line = sys.stdin.readline()
            if actual_line == "":
                break
            actualFile.write(actual_line)
            parts = json.loads(actual_line)
            for word in parts['words']:
                if word['word'] == '' or re.match("[\s]", word['word']):
                    word['word'] = '?????'
            words = [word['word'] for word in parts['words']]
            sent = " ".join(words) + '\n'
            if first_word_lower:
                sent = sent[0].lower() + sent[1:]
            inputFile.write(sent.encode('utf-8', 'ignore'))
        actualFile.close()
        inputFile.close()
        outFile.close()
        
        actualFileName = actualFile.name
        outFileName = outFile.name
        inputFileName = inputFile.name
        
        running_command = "cat %s | %s > %s" % (inputFileName, pos_command, outFileName)
        sys.stderr.write(running_command + '\n')
        os.system(running_command)
        
        outFile = open(outFileName)
        actualFile = open(actualFileName)
        
        inp = actualFile.readline()
        line = outFile.readline()
        
        while inp != "" and line != "":
            parts = json.loads(inp)
            for word in parts['words']:
                if word['word'] == '' or re.match("[\s]", word['word']):
                    word['word'] = '?????'
            word_pos_list = line.strip().split()
            if add_poss:
                prev_pos = ""
                insert_positions = []
                for i, word_pos in enumerate(word_pos_list):
                    (word, pos) = word_pos.split("|")
                    parts['words'][i]['pos'] = pos
                    if (prev_pos == "NNP" or prev_pos == "NNPS") and (pos == "NN" or pos == "NNS"):
                        insert_positions.append(i)
                    prev_pos = pos
                for position, increment in enumerate(insert_positions):
                    for i, entity in enumerate(parts['entities']):
                        if entity['index'] >= position + increment:
                            entity['index'] = entity['index'] + 1
                    parts['words'].insert(position + increment, {"word": "'s", "ner": "0", "pos": "POS"})  
                    # word_pos_list.insert(position + increment, "'s|POS")
                for entity in parts['entities']:
                    if parts['words'][entity['index']]['pos'] != "NNP" and parts['words'][entity['index']]['pos'] != "NNPS": 
                        parts['words'][entity['index']]['pos'] = 'NNP'
                        # word_pos_list[entity['index']] = parts['words'][entity['index']]['word'] + "|" + "NNP"
            
            
            # Merge all adjacent numbers into a single number
            positions_decrement = []
            prev = "";
            new_words = []
            decrement = 0;
            for word in parts['words']:
                if word['pos'] == 'CD' and prev == "CD":
                    new_words[-1]['word'] += "_" + word['word']
                    decrement += 1
                else:
                    new_words.append(word) 
                positions_decrement.append(decrement)    
                prev = word['pos']
            parts['words'] = new_words
            
            for entity in parts['entities']:
                entity['index'] = entity['index'] - positions_decrement[entity['index']] 
            
            print json.dumps(parts)
            inp = actualFile.readline()
            line = outFile.readline()
        
        os.remove(actualFileName)
        os.remove(inputFileName)
        os.remove(outFileName)
        # os.remove(depOutFileName)
        
        sys.stderr.write("Inputfile " + inputFileName + ' ' + " Outfile " + outFileName)  # + " depOutfile " + depOutFileName + '\n');
        sys.stderr.write(running_command + '\n')
        # sys.stderr.write("Failed count %s\n" % (count))

# exit()


if __name__ == "__main__":
    if sys.argv[1] == "questions":
        parse_sentences("./tools/candc_nbest/models/questions/")
    if sys.argv[1] == "questions_lower":
        parse_sentences("./tools/candc_nbest/models/questions/", True)
    elif sys.argv[1] == "normal":
        parse_sentences("./tools/candc_nbest/models")
    elif sys.argv[1] == "normal_lower":
        parse_sentences("./tools/candc_nbest/models", True)
    elif sys.argv[1] == "questions_modified":
        parse_sentences("./tools/candc_nbest/models.mod/questions/", True)
