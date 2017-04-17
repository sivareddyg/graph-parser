import sys
import json
import re

pp = []
conj = []
control = []
relatives = []

for i, line in enumerate(sys.stdin):
    sentence = json.loads(line)
    if "synPars" in sentence and len(sentence['synPars']) > 0:
        synPar = sentence['synPars'][0]['synPar']
        if synPar.find("PP") != -1 or synPar.find("((S\NP)/(S\NP))/NP") != -1 or synPar.find("(NP\NP)/NP") != -1:
            pp.append(i)
        if synPar.find("conj") != -1:
            conj.append(i)
        if re.search("\(N[P]?[\\\/]N[P]?\)[\\\/]\(S(\[[^\(\)\\\/]+\])?[\\\/]N[P]?\)", synPar) or re.search("\|S(\[[^\(\)\\\/]+\])?[\\\/]S(\[[^\(\)\\\/]+\])? ", synPar):
            relatives.append(i)
        if re.search("\(S(\[[^\(\)\\\/]+\])?[\\\/]N[P]?\)\/\(S(\[to\])[\\\/]N[P]?\)", synPar):
            control.append(i)

constructions = {"subordinate":relatives, "conj": conj, "pp": pp, "control": control}           

print json.dumps(constructions)
