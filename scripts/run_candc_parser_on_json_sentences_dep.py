'''
Created on 6 Sep 2013

@author: Siva Reddy
'''

import json
import os
import re
import sys
import tempfile


def parse_sentences(model, first_word_lower=False):
    command = """./tools/candc_nbest/bin/candc \
--candc-parser-kbest=1 \
--models %s \
--candc-printer=ccgbank""" % (model)

    dep_command = """./tools/candc_nbest/bin/candc \
--candc-parser-kbest=1 \
--models %s \
--candc-printer=deps""" % (model)

    actual_line = " "
    while actual_line != "":
        actual_file = tempfile.NamedTemporaryFile(
            prefix="siva_candc_", dir="../working", delete=False)
        input_file = tempfile.NamedTemporaryFile(
            prefix="siva_candc_", dir="../working", delete=False)
        out_file = tempfile.NamedTemporaryFile(
            prefix="siva_candc_", dir="../working", delete=False)
        depout_file = tempfile.NamedTemporaryFile(
            prefix="siva_candc_", dir="../working", delete=False)

        sys.stderr.write("Temporary Input: %s\n" % (input_file.name))
        sys.stderr.write("Temporary Output: %s\n" % (out_file.name))
        sys.stderr.write("Temporary Dep Output: %s\n" % (depout_file.name))

        count = 0
        while actual_line != "":
            count += 1
            if count % 10 == 0:
                break
            actual_line = sys.stdin.readline()
            if actual_line == "":
                break
            actual_file.write(actual_line)
            parts = json.loads(actual_line)
            if "synPars" in parts:
                continue
            words = [word['word'] for word in parts['words']]
            sent = " ".join(words) + '\n'
            if first_word_lower:
                sent = sent[0].lower() + sent[1:]
            input_file.write(sent.encode('utf-8', 'ignore'))
        actual_file.close()
        input_file.close()
        out_file.close()

        running_command = "cat %s | %s > %s" % (
            input_file.name, command, out_file.name)
        sys.stderr.write(running_command + '\n')
        os.system(running_command)

        dep_running_command = "cat %s | %s > %s" % (
            input_file.name, dep_command, depout_file.name)
        sys.stderr.write(dep_running_command + '\n')
        os.system(dep_running_command)

        actual_file_name = actual_file.name
        out_file_name = out_file.name
        input_file_name = input_file.name
        depout_file_name = depout_file.name

        out_file = open(out_file_name)
        depout_file = open(depout_file_name)
        input_file = open(input_file_name)

        count = 0
        linecount = 1

        actual_file = open(actual_file_name)
        inp = actual_file.readline()
        line = out_file.readline()
        score_line = ""
        while inp != "" and line != "":
            parts = json.loads(inp)
            if "synPars" in parts:
                print inp.strip()
                inp = actual_file.readline()
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
                    inp = actual_file.readline()
                    if "synPars" not in parts:
                        linecount += 1
                    sys.stderr.write(
                        "Cannot parse 1st Iteration: %s\n" % (sent))
                    count += 1
                if inp == "":
                    break
                parts = json.loads(inp)
                parses = []
                parse = out_file.readline().strip()
                while 1:
                    dep_line = depout_file.readline()
                    while not re.match("score", dep_line):
                        dep_line = depout_file.readline()

                    dep_line = depout_file.readline()
                    depParse = ""
                    while dep_line != "" and dep_line.strip() != "":
                        depParse += dep_line
                        dep_line = depout_file.readline()

                    score = float(
                        re.findall("score = ([0-9\.-]+)", score_line)[0])
                    if parse.startswith("(<T S[dcl] ") \
                            or parse.startswith("(<T S[pss] ") \
                            or parse.startswith("(<T S[pt] ") \
                            or parse.startswith("(<T S[b] ") \
                            or parse.startswith("(<T S[ng] "):
                        parses.append(
                            {'synPar': parse, 'score': score,
                             'depParse': depParse})
                    line = out_file.readline()
                    if line == "" or line.strip() == "":
                        break
                    score_line = line
                    out_file.readline()
                    parse = out_file.readline().strip()
                if parses != []:
                    parts['synPars'] = parses
                    print json.dumps(parts)
                else:
                    print inp.strip()
                    words = [word['word'] for word in parts['words']]
                    sent = " ".join(words) + '\n'
                    sys.stderr.write("Parse is not sentence: %s\n" % (sent))
                    count += 1
                inp = actual_file.readline()
                linecount += 1
            line = out_file.readline()

        while inp != "":
            print inp.strip()
            inp = actual_file.readline()

        out_file.close()

        os.remove(input_file_name)
        os.remove(actual_file_name)
        os.remove(out_file_name)
        os.remove(depout_file_name)

        sys.stderr.write("input_file " + input_file_name + ' ' +
                         " out_file " + out_file_name + " depout_file " +
                         depout_file_name + '\n')

        sys.stderr.write(running_command + '\n')

        sys.stderr.write("Failed count %s\n" % (count))

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
