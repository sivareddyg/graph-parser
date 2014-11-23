'''
Created on 6 Sep 2013

@author: Siva Reddy
'''

import sys
import json
import os
import re
# from subprocess import Popen, PIPE
import tempfile


def parse_sentences(model, first_word_lower = False):
    command = """./tools/candc_nbest/bin/candc \
--candc-parser-kbest=1 \
--models %s \
--candc-printer=ccgbank""" %(model)


    dep_command = """./tools/candc_nbest/bin/candc \
--candc-parser-kbest=1 \
--models %s \
--candc-printer=deps""" %(model)

    
    actualFile = tempfile.NamedTemporaryFile(prefix = "siva_candc_", dir = "./working", delete = False);
    inputFile = tempfile.NamedTemporaryFile(prefix = "siva_candc_", dir = "./working", delete = False);
    outFile = tempfile.NamedTemporaryFile(prefix = "siva_candc_", dir = "./working", delete = False);
    depOutFile = tempfile.NamedTemporaryFile(prefix = "siva_candc_", dir = "./working", delete = False);
    
    
    sys.stderr.write("Temporary Input: %s\n" %(inputFile.name))
    sys.stderr.write("Temporary Output: %s\n" %(outFile.name))
    sys.stderr.write("Temporary Dep Output: %s\n" %(depOutFile.name))
    
    for line in sys.stdin:
        actualFile.write(line)
        parts = json.loads(line)
        if parts.has_key("synPars"):
            continue
        words = [word['word'] for word in parts['words']]
        sent = " ".join(words) + '\n'
        if first_word_lower:
            sent = sent[0].lower() + sent[1:]
        inputFile.write(sent.encode('utf-8', 'ignore'))
    actualFile.close()
    inputFile.close()
    outFile.close()
    
    running_command = "cat %s | %s > %s" %(inputFile.name, command, outFile.name)
    sys.stderr.write(running_command + '\n')
    os.system(running_command)
    
    
    dep_running_command = "cat %s | %s > %s" %(inputFile.name, dep_command, depOutFile.name)
    sys.stderr.write(dep_running_command + '\n')
    os.system(dep_running_command)
    
    actualFileName = actualFile.name
    outFileName = outFile.name
    inputFileName = inputFile.name
    depOutFileName = depOutFile.name
    
    outFile = open(outFileName)
    depOutFile = open(depOutFileName)
    inputFile = open(inputFileName)
    
    count = 0
    linecount = 1
    prev_line = outFile.readline()
    
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
        if re.match("score", line):
            score_line = line
        if re.match("ID=", line):
            id = int(re.findall("ID=([0-9]+)", line)[0])
            # print linecount, id
            # print line
            while id != linecount and inp != "":
                print inp.strip()
                parts = json.loads(inp)
                words = [word['word'] for word in parts['words']]
                sent = " ".join(words) + '\n'
                inp = actualFile.readline()
                if not parts.has_key("synPars"):
                    linecount += 1
                sys.stderr.write("Cannot parse 1st Iteration: %s\n" %(sent))
                count += 1
            if inp == "":
                break
            parts = json.loads(inp)
            parses = []
            parse = outFile.readline().strip()
            while 1:
                depLine = depOutFile.readline()
                while not re.match("score", depLine):
                    depLine = depOutFile.readline()
                
                depLine =  depOutFile.readline()
                depParse = "";
                while depLine != "" and depLine.strip() != "":
                    depParse +=  depLine
                    depLine = depOutFile.readline()
                
                score = float(re.findall("score = ([0-9\.-]+)", score_line)[0])
                if parse.startswith("(<T S[dcl] ") or parse.startswith("(<T S[pss] ") or parse.startswith("(<T S[pt] ") or parse.startswith("(<T S[b] ") or parse.startswith("(<T S[ng] "):
                    parses.append({'synPar' : parse, 'score' : score, 'depParse' : depParse})
                line = outFile.readline()
                if line == "" or line.strip() == "":
                    break
                score_line = line
                outFile.readline()
                parse = outFile.readline().strip()
            if parses != []:
                parts['synPars'] = parses
                print json.dumps(parts)
            else:
                print inp.strip()
                words = [word['word'] for word in parts['words']]
                sent = " ".join(words) + '\n'
                sys.stderr.write("Parse is not sentence: %s\n" %(sent))
                count += 1
            inp = actualFile.readline()
            linecount += 1
        line = outFile.readline()
    
    while inp != "":
        print inp.strip()
        inp = actualFile.readline()
    
    outFile.close()
    
    os.remove(inputFileName)
    os.remove(actualFileName)
    os.remove(outFileName)
    os.remove(depOutFileName)
    
    sys.stderr.write("Inputfile " + inputFileName + ' ' + " Outfile " + outFileName + " depOutfile " + depOutFileName + '\n');
    
    sys.stderr.write(running_command + '\n')
    
    sys.stderr.write("Failed count %s\n" %(count))

# exit()


if __name__ == "__main__":
    if sys.argv[1] == "questions":
        parse_sentences("./tools/candc_nbest/models/questions/")
    if sys.argv[1] == "questions_lower":
        parse_sentences("./tools/candc_nbest/models/questions/", True)
    elif sys.argv[1] == "normal":
        parse_sentences("./tools/candc_nbest/models/boxer")
    elif sys.argv[1] == "normal_lower":
        parse_sentences("./tools/candc_nbest/models/boxer", True)
    elif sys.argv[1] == "questions_modified":
        parse_sentences("./tools/candc_nbest/models.mod/questions/", True)
