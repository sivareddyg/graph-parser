# -*- coding: utf-8 -*-
#{
#"entities":[
#  {"index":0,"entity":"m.08z129"}
#  ],
#"words":[
#  {"ner":"O","word":"SBC_Communications","pos":"NNP"},
#  {"ner":"O","word":"acquired","pos":"VBD"},
#  {"ner":"O","word":"_blank_","pos":"NN"},
#  {"ner":"O","word":"in","pos":"IN"},
#  {"ner":"DATE","word":"2000","pos":"NNP"},
#  {"ner":"O","word":".","pos":"."}
#  ],
#"synPars":
#  [
#  {"synPar":"(\u003cT S[dcl] rp 0 2\u003e (\u003cT S[dcl] ba 1 2\u003e (\u003cT NP lex 0 1\u003e (\u003cL N SBC_Communications SBC_Communications NNP I-ORG I-NP N\u003e)) (\u003cT S[dcl]\\NP ba 0 2\u003e (\u003cT S[dcl]\\NP fa 0 2\u003e (\u003cL (S[dcl]\\NP)/NP acquired acquire VBD O I-VP (S[dcl]\\NP)/NP\u003e) (\u003cT NP lex 0 1\u003e (\u003cL N _blank_ _blank_ NN O I-NP N\u003e))) (\u003cT (S\\NP)\\(S\\NP) fa 0 2\u003e (\u003cL ((S\\NP)\\(S\\NP))/NP in in IN O I-PP ((S[X]\\NP)\\(S[X]\\NP))/NP\u003e) (\u003cT NP lex 0 1\u003e (\u003cL N 2000 2000 NNP I-DAT I-NP N\u003e))))) (\u003cL . . . . O O .\u003e))", "score":20.387499999999999}
#  ],
#"sentence":"SBC_Communications acquired _blank_ in March_2000 .", 
#"answerSubset":["m.0by5r3"]
#}

import os,sys,json,gzip,copy,re,random
random.seed(ord('i'))
withParses = True

def replace(e,j):
  # Remove entity
  entity = j["entities"].pop(e)
  if "m." in entity["entity"]:
    j["answerSubset"] = [entity["entity"]]
  else:
    j["answer"] = entity["entity"]
  word = j["words"][int(entity["index"])]["word"]
  # Set word to blank
  j["words"][int(entity["index"])]["word"] = "_blank_"
  j["words"][int(entity["index"])]["pos"] = "NNP"
  j["words"][int(entity["index"])]["supertags"] = ["N"]
  if withParses and "synPars" in j:
    # Set lex in parses to blank
    for synPar in j["synPars"]:
      parse =  synPar["synPar"]
      parse = parse.split("(<L")
      try:
        parse[int(entity["index"])+1] = re.sub(r'^ NP ' + word + '[^>]*?>',\
                                      ' NP _blank_ _blank_ NNP O I-NP NP>', parse[int(entity["index"]) + 1])
        parse[int(entity["index"])+1] = re.sub(r'^ N ' + word + '[^>]*?>',\
                                      ' N _blank_ _blank_ NNP O I-NP N>', parse[int(entity["index"]) + 1])
        synPar["synPar"] = "(<L".join(parse)
      except:
        print "Bad expression: " + parse[int(entity["index"]) + 1]
        del j["synPars"]
        break
  else:
    if "synPars" in j:
        del j["synPars"]
  # Replace in sentence
  sent = j["sentence"].split()
  sent[int(entity["index"])] = "_blank_"
  j["sentence"] = " ".join(sent)
  return json.dumps(j)

for line in sys.stdin:
  j = json.loads(line)
  e = random.randint(0,len(j["entities"]) - 1)
  print replace(e,copy.deepcopy(j))
