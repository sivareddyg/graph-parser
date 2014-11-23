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

#sys.stdin = open("working/tmp.txt")

def parse_sentences(model, first_word_lower=False):
    command = """java -cp tools/easyccg/easyccg.jar uk.ac.ed.easyccg.main.EasyCCG -m %s -r S[wq] S[q] -i POSandNERtagged -o extended -n 10""" %(model)

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
            if parts.has_key("synPars"):
                continue
            sent = ""
            for word in parts['words']:
                if word['word'] == '' or re.match("[\s]", word['word']):
                    word['word'] = '?????'
                sent += word['word'] + '|' + word['pos'] + '|' + word['candcner'] + " "
            sent = sent[:-1]
            if first_word_lower:
                sent = sent[0].lower() + sent[1:]
            inputFile.write(sent.encode('utf-8', 'ignore') + '\n')
        actualFile.close()
        inputFile.close()
        
        
        actualFileName = actualFile.name
        inputFileName = inputFile.name
        outFileName = outFile.name
        
        running_command = "cat %s | %s > %s" % (inputFileName, command, outFileName)
        sys.stderr.write(running_command + '\n')
        os.system(running_command)
        
        # exit()
        
        outFile = open(outFileName)
        
        count = 0
        linecount = 1
        
        actualFile = open(actualFileName)
        inp = actualFile.readline()
        line = outFile.readline()
        score_line = ""
        while inp != "" and line != "":
            parts = json.loads(inp)
            if parts.has_key("synPars"):
                print inp.strip()
                inp = actualFile.readline()
                continue
            for word in parts['words']:
                if word['word'] == '' or re.match("[\s]", word['word']):
                    word['word'] = '?????'
            if re.match("score", line):
                score_line = line
            if re.match("ID=", line):
                id = int(re.findall("ID=([0-9]+)", line)[0])
                # print linecount, id
                # print line
                while id != linecount and inp != "":
                    # i.e. did not parse the sentence
                    print inp.strip()
                    if not parts.has_key("synPars"):
                        words = [word['word'] for word in parts['words']]
                        sent = " ".join(words) + '\n'
                        sys.stderr.write("Cannot parse : %s\n" % (sent))
                        count += 1
                    inp = actualFile.readline()
                    parts = json.loads(inp)
                    if not parts.has_key("synPars"):
                        linecount += 1
                    
                if inp == "":
                    break
                parts = json.loads(inp)
                parses = []
                line = outFile.readline().strip()
                while line != "":
                    #score = float(re.findall("score = ([0-9\.-]+)", score_line)[0])
                    score = 1.0
                    if line.startswith("(<T S[wq] ") or line.startswith("(<T S[q] "):# or parse.startswith("(<T S[qem] "):
                        parses.append({'synPar' : line, 'score' : score})  # , 'depParse' : depParse})
                    line = outFile.readline().strip()
                    if re.match("ID=", line):
                        id = int(re.findall("ID=([0-9]+)", line)[0])
                        if id != linecount:
                            break
                    
                if parses != []:
                    parts['synPars'] = parses
                    print json.dumps(parts)
                else:
                    print inp.strip()
                    words = [word['word'] for word in parts['words']]
                    sent = " ".join(words) + '\n'
                    sys.stderr.write("Parse is not sentence: %s\n" % (sent))
                    count += 1
                inp = actualFile.readline()
                linecount += 1
            # line = outFile.readline()
        
        while inp != "":
            parts = json.loads(inp)
            if not parts.has_key('synPars'):
                words = [word['word'] for word in parts['words']]
                sent = " ".join(words) + '\n'
                sys.stderr.write("Cannot parse : %s\n" % (sent))
                count += 1
            print inp.strip()
            inp = actualFile.readline()
        
        outFile.close()
        
        #os.remove(inputFileName)
        #os.remove(actualFileName)
        #os.remove(outFileName)
        # os.remove(depOutFileName)
        
        sys.stderr.write("Inputfile " + inputFileName + ' ' + " Outfile " + outFileName)  # + " depOutfile " + depOutFileName + '\n');
        sys.stderr.write(running_command + '\n')
        sys.stderr.write("Failed count %s\n" % (count))

# exit()


if __name__ == "__main__":
    if sys.argv[1] == "questions":
        parse_sentences("./tools/easyccg/model_questions -s --supertaggerbeam 0 ")
    if sys.argv[1] == "questions_lower":
        parse_sentences("./tools/easyccg/model_questions -s --supertaggerbeam 0 ", True)
    elif sys.argv[1] == "normal":
        parse_sentences("./tools/easyccg/model")
    elif sys.argv[1] == "normal_lower":
        parse_sentences("./tools/easyccg/model", True)
    elif sys.argv[1] == "rebank":
        parse_sentences("./tools/easyccg/model_rebank")
    elif sys.argv[1] == "rebank_lower":
        parse_sentences("./tools/easyccg/model_rebank", True)