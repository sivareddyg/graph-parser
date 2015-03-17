'''
Created on 8 Mar 2015

@author: siva
'''

import json
import sys

english_annotations = {}
for line in open(sys.argv[1]):
    line = json.loads(line)
    english_annotations[line['utterance'].replace("\\", "")] = line

for line in sys.stdin:
    spanish = json.loads(line)
    english = english_annotations[spanish['english'].replace("\\", "")]
    # {"url": "http://www.freebase.com/view/en/jamaica", "targetValue": "(list (description \"Jamaican Creole English Language\") (description \"Jamaican English\"))", "utterance": "what does jamaican people speak?"}
    spanish['url'] = english['url']
    spanish['targetValue'] = english['targetValue']
    print json.dumps(spanish)