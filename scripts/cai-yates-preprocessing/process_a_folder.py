'''
Created on 20 Sep 2013

@author: Siva Reddy
'''

import os
import sys
import re

inp_folder = sys.argv[1]
out_folder = sys.argv[2]

os.system('mkdir -p %s' %(out_folder))
files = os.listdir(inp_folder)
print files

for file in files:
    if re.match("\.", file):
       continue
    command = """python scripts/cai-yates-preprocessing/tokenise.py data/cai-yates-2013/question-and-logical-form-917/dataset-all-917.sparql.txt < %s \
        | python scripts/cai-yates-preprocessing/tag_named_entities.py data/cai-yates-2013/fixed-np-manually.txt \
        | java -cp .:tools/stanford-ner-2012-11-11/stanford-ner-2012-11-11.jar:tools/stanford-ner-2012-11-11/gson-2.2.2.jar:tools/stanford-ner-2012-11-11/guava-14.0.1.jar:scripts NerJsonInputData \
        | python scripts/cleaning/ner_post_process.py \
        | python scripts/run_candc_parser_on_json_sentences_questions.py questions \
        |  python scripts/run_candc_parser_on_json_sentences_questions.py questions_lower \
        | python scripts/run_candc_parser_on_json_sentences_questions.py normal \
        | python scripts/run_candc_parser_on_json_sentences_questions.py normal_lower \
        > %s
    """ %(os.path.join(inp_folder, file), os.path.join(out_folder, file))
    os.system(command)