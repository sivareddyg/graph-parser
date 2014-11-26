'''
Created on Nov 24, 2014

@author: sivareddy
'''

import sys
import simplejson

for line in sys.stdin:
  json_sent = simplejson.loads(line)
  words = [word['word'] for word in json_sent['words']]
  for entity in json_sent['entities']:
    entity_name = words[entity['index']]
    entity_mid = entity['entity']
    print "%s\t%s" %(entity_name, entity_mid)
    if entity_name.strip(".") != entity_name:
      print "%s\t%s" %(entity_name.strip("."), entity_mid)