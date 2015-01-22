'''
Created on Nov 22, 2014

@author: sivareddy
'''

import sys
import simplejson
import re

measure_types = { 0: "type.int", 1: "type.float", 3: "type.datetime", 4: "type.datetime"}

def document_to_graphparser_input(document):
  gdoc = {}
  
  if 'text' in document:
    gdoc['sentence'] = document['text']
  
  # Process entities
  token_to_mention_or_measure_head = {}  # {int : int}
  head_to_freebase_id = {}  # {int: str}
  head_to_phrase = {}  # { int : str}
  if 'entity' in document:
    for entity in document['entity']:
      if 'mention' in entity:
        for mention in entity['mention']:
          if 'type' not in mention or mention['type'] == 0:  # mention is a named entity
            head = mention['phrase']['head']
            head_to_phrase[head] = entity['name']
            for token_index in range(mention['phrase']['start'], mention['phrase']['end'] + 1):
              if token_index != head:
                token_to_mention_or_measure_head[token_index] = head
            
            if 'profile' in entity:
              profile =  entity['profile']
              if 'identifier' in profile:
                for identifier in profile['identifier']:
                  if 'domain' in identifier and identifier['domain'] == 2:  # FREEBASE_MID
                    head_to_freebase_id[head] = identifier['id'].strip("/").replace("/", ".")
            
  if 'measure' in document:
    for measure in document['measure']:
      # If there is a head, use it, else treat the first word in the phrase as head.
      head = measure['phrase']['head']  if 'head' in measure else measure['phrase']['start']
      head_to_phrase[head] = str(measure['value']) if 'value' in measure else measure['phrase']['start']
      for token_index in range(measure['phrase']['start'], measure['phrase']['end'] + 1):
        if token_index != head:
          token_to_mention_or_measure_head[token_index] = head
      if 'type' in measure:
        measure_type = measure['type']
        head_to_freebase_id[head] = measure_types.get(measure_type, 'type.float')
      
  old_token_index_to_new = {}
  # Populate words
  gdoc["words"] = []
  words = gdoc["words"]
  new_word_index = 0
  for token_index, token in enumerate(document['token']):
    old_token_index_to_new[token_index] = new_word_index
     
    # Store only the heads
    if token_index in token_to_mention_or_measure_head: continue
    new_word_index += 1
    
    words.append({})
    word = words[-1]
    for key in token:
      if type(key) is not str:
        continue
      if key[0] == "[":
        continue
      if key == "tag":
        word["pos"] = token[key]
      else:
        word[key] = token[key]
    if 'lemma' not in token:
      if re.match("NNP", word["pos"]): 
        word['lemma'] = word['word']
      else:
        word['lemma'] = word['word'].lower()
      
    if token_index in head_to_phrase:
      word['word'] = "_".join(head_to_phrase[token_index].split())
      word['lemma'] = word['word']
  
  # Populate entities in graphparser doc
  gdoc['entities'] = []
  entities = gdoc['entities']
  for head_token in sorted(head_to_freebase_id.keys()):
    entities.append({})
    entity = entities[-1]
    entity['index'] = old_token_index_to_new[head_token]
    entity['entity'] = head_to_freebase_id[head_token]
    entity['name'] = "_".join(head_to_phrase[head_token].split())
  
  if '[nlp_saft.GraphParserLambda.extension]' in document:
    lambdas = document['[nlp_saft.GraphParserLambda.extension]']
    if 'dependency_lambda' in lambdas:
      gdoc['dependency_lambda'] = []
      dependency_lambdas = lambdas['dependency_lambda']
      for dependency_lambda in dependency_lambdas:
        new_lambda = set()
        process_lambda(words, head_to_freebase_id, old_token_index_to_new, dependency_lambda, new_lambda)
        gdoc['dependency_lambda'].append(list(new_lambda))
        # print new_lambda
        
    if 'ccg_lambda' in lambdas:
      gdoc['ccg_lambda'] = []
      ccg_lambdas = lambdas['ccg_lambda']
      for ccg_lambda in ccg_lambdas:
        new_lambda = set()
        process_lambda(words, head_to_freebase_id, old_token_index_to_new, ccg_lambda, new_lambda)
        gdoc['ccg_lambda'].append(list(new_lambda))
  return gdoc

def get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, var):
  if re.match("[0-9]+$", var): return int(var)
  if var in var_to_token_index: return var_to_token_index[var]
  word = {'word': "", 'pos': ""}
  words.append(word)
  var_to_token_index[var] = len(words) - 1
  old_token_index_to_new[var_to_token_index[var]] = var_to_token_index[var] 
  return var_to_token_index[var]

def process_lambda(words, head_to_freebase_id, old_token_index_to_new, old_lambda, new_lambda):
  if not isinstance(new_lambda, set): return
  
  var_to_token_index = {}
  
  basic_expression_pattern = re.compile("\([^\(\)]+\)")
  type_pattern = re.compile("\(([^\s]+) ([^\s]+):s ([^\s]+):n\)")
  event_entity_pattern = re.compile("\(([^\s]+) ([^\s]+):e ([^\s]+):n\)")
  event_event_pattern = re.compile("\(([^\s]+) ([^\s]+):e ([^\s]+):e\)")
  eventmod_pattern = re.compile("\(([^\s]+) ([^\s]+):s ([^\s]+):e\)")
  count_pattern = re.compile("\(([^\s]+) ([^\s]+):n ([^\s]+):n\)")
  unary_entity_pattern = re.compile("\(([^\s]+) ([^\s]+):n\)")
  unary_event_pattern = re.compile("\(([^\s]+) ([^\s]+):e\)")
  
  basic_expressions = basic_expression_pattern.findall(old_lambda)
  for basic_expression in basic_expressions:
    m = type_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg2 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(3))
      
      # Do not have predicates that are named entities.
      if arg1 == arg2 and arg2 in head_to_freebase_id:
        continue
      arg1 = str(old_token_index_to_new[arg1]) + ":s"
      if int(arg2) in head_to_freebase_id:
        arg2 = str(old_token_index_to_new[arg2]) + ":" + head_to_freebase_id[int(arg2)]
      else:
        arg2 = str(old_token_index_to_new[arg2]) + ":x"
      if predicate.lower() not in ['what']:
        new_lambda.add("%s(%s , %s)" %(predicate.lower(), str(arg1), str(arg2)))
      continue
      
    m = event_entity_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg2 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(3))
      
      arg1 = str(old_token_index_to_new[arg1]) + ":e"
      if int(arg2) in head_to_freebase_id:
        arg2 = str(old_token_index_to_new[arg2]) + ":" + head_to_freebase_id[int(arg2)]
      else:
        arg2 = str(old_token_index_to_new[arg2]) + ":x"
      new_lambda.add("%s(%s , %s)" %(predicate.lower(), str(arg1), str(arg2)))
      continue
    
    m = event_event_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg2 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(3))
      
      arg1 = str(old_token_index_to_new[arg1]) + ":e"
      arg2 = str(old_token_index_to_new[arg2]) + ":e"
      new_lambda.add("%s(%s , %s)" %(predicate.lower(), str(arg1), str(arg2)))
      continue
    
    m = eventmod_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg2 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(3))
      
      arg1 = str(old_token_index_to_new[arg1]) + ":s"
      arg2 = str(old_token_index_to_new[arg2]) + ":e"
      new_lambda.add("%s(%s , %s)" %(predicate.lower(), str(arg1), str(arg2)))
      continue
    
    m = count_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg2 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(3))
      
      if arg1 in head_to_freebase_id:
        arg1 = str(old_token_index_to_new[arg1]) + ":" + head_to_freebase_id[int(arg1)]
      else:
        arg1 = str(old_token_index_to_new[arg1]) + ":x"
        
      if arg2 in head_to_freebase_id:
        arg2 = str(old_token_index_to_new[arg2]) + ":" + head_to_freebase_id[int(arg2)]
      else:
        arg2 = str(old_token_index_to_new[arg2]) + ":x"
      new_lambda.add("%s(%s , %s)" %(predicate, str(arg1), str(arg2)))
      continue
      
    m = unary_entity_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      if arg1 in head_to_freebase_id:
        arg1 = str(old_token_index_to_new[arg1]) + ":" + head_to_freebase_id[int(arg1)]
      else:
        arg1 = str(old_token_index_to_new[arg1]) + ":x"
      new_lambda.add("%s(%s)" %(predicate, str(arg1)))
      continue
      
    m = unary_event_pattern.match(basic_expression)
    if m:
      predicate = m.group(1)
      arg1 = get_var_to_word_index(words, old_token_index_to_new, var_to_token_index, m.group(2))
      arg1 = str(old_token_index_to_new[arg1]) + ":e"
      new_lambda.add("%s(%s)" %(predicate, str(arg1)))
      continue

if __name__ == "__main__":
  #line = '''{"annotated_phrase": [], "author": [], "constituency_root": [], "text": "James Cameron directed the movie Titanic in 1997\\n", "constituency_node": [], "entity_label": [], "entity": [{"profile": {"name": "James Cameron", "keyword": [], "nature": 4, "attribute": [], "alternate": [], "mid": "/m/03_gd", "related": [], "gender": 0, "identifier": [{"domain": 2, "id": "/m/03_gd"}, {"domain": 0, "id": "James Cameron"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/James_Cameron"}, {"domain": 5, "id": "James Cameron"}], "type": "PER", "id": 842411, "reference": []}, "info": {"[nlp_saft.KBestEntityList]": {"result": [{"profile": {"name": "James Cameron", "keyword": [], "nature": 4, "attribute": [], "alternate": [], "mid": "/m/03_gd", "related": [], "gender": 0, "identifier": [{"domain": 2, "id": "/m/03_gd"}, {"domain": 0, "id": "James Cameron"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/James_Cameron"}, {"domain": 5, "id": "James Cameron"}], "type": "PER", "id": 842411, "reference": []}, "context_score": 43.021873, "type_score": 25.90625, "context_posterior": 0.01065, "name_score": 0.98042202, "prior": 9.9999997e-06, "score": 0.999897}, {"profile": {"name": "James Cameron", "keyword": [], "nature": 4, "attribute": [], "disambiguation": "activist", "alternate": [], "mid": "/m/0f6rk1", "related": [], "gender": 0, "identifier": [{"domain": 2, "id": "/m/0f6rk1"}, {"domain": 0, "id": "James Cameron (activist)"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/James_Cameron_(activist)"}, {"domain": 5, "id": "James Cameron"}], "type": "PER", "id": 3062408, "reference": []}, "context_score": 5.2437501, "type_score": 3.6875, "context_posterior": 1e-06, "name_score": 0.004776, "prior": 0, "score": 8.5e-05}, {"profile": {"name": "James Cameron", "keyword": [], "nature": 2, "attribute": [], "disambiguation": "book", "alternate": [], "mid": "/m/0c3t8r3", "related": [], "identifier": [{"domain": 2, "id": "/m/0c3t8r3"}, {"domain": 5, "id": "James Cameron"}], "type": "NON", "id": 11690545, "reference": []}, "context_score": 85.400002, "type_score": 1, "context_posterior": 0, "name_score": 0.000158, "prior": 0, "score": 1.2e-05}]}}, "name": "James Cameron", "entity_type": "PER", "gender": 0, "representative_mention": 0, "entity_type_probability": [], "mention": [{"phrase": {"start": 0, "head": 1, "end": 1}, "kind": 1, "type": 0}], "type": []}, {"name": "movie", "entity_type": "NON", "representative_mention": 0, "entity_type_probability": [], "mention": [{"phrase": {"start": 4, "head": 4, "end": 4}, "kind": 1, "type": 1}], "type": []}, {"profile": {"name": "Titanic", "keyword": [], "nature": 2, "attribute": [], "disambiguation": "1997 film", "alternate": [], "mid": "/m/0dr_4", "related": [], "identifier": [{"domain": 2, "id": "/m/0dr_4"}, {"domain": 0, "id": "Titanic (1997 film)"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/Titanic_(1997_film)"}, {"domain": 5, "id": "Titanic"}], "type": "NON", "id": 3002125, "reference": []}, "info": {"[nlp_saft.KBestEntityList]": {"result": [{"profile": {"name": "Titanic", "keyword": [], "nature": 2, "attribute": [], "disambiguation": "1997 film", "alternate": [], "mid": "/m/0dr_4", "related": [], "identifier": [{"domain": 2, "id": "/m/0dr_4"}, {"domain": 0, "id": "Titanic (1997 film)"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/Titanic_(1997_film)"}, {"domain": 5, "id": "Titanic"}], "type": "NON", "id": 3002125, "reference": []}, "context_score": 110.02969, "type_score": 8.8242188, "context_posterior": 0.006083, "name_score": 0.64985001, "prior": 6.0000002e-06, "score": 0.97275102}, {"profile": {"name": "Titanic: Music from the Motion Picture", "keyword": [], "nature": 2, "attribute": [], "alternate": [], "mid": "/m/01hw5ff", "related": [], "identifier": [{"domain": 2, "id": "/m/01hw5ff"}, {"domain": 0, "id": "Titanic: Music from the Motion Picture"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/Titanic:_Music_from_the_Motion_Picture"}, {"domain": 5, "id": "Titanic: Music from the Motion Picture"}], "type": "NON", "id": 133436, "reference": []}, "context_score": 55.05703, "type_score": 8.8242188, "context_posterior": 6.0999999e-05, "name_score": 0.010415, "prior": 0, "score": 0.007801}, {"profile": {"name": "Titanic", "keyword": [], "nature": 2, "attribute": [], "disambiguation": "musical", "alternate": [], "mid": "/m/06v5wl", "related": [], "identifier": [{"domain": 2, "id": "/m/06v5wl"}, {"domain": 0, "id": "Titanic (musical)"}, {"domain": 4, "id": "http://en.wikipedia.org/wiki/Titanic_(musical)"}, {"domain": 5, "id": "Titanic"}], "type": "NON", "id": 2015604, "reference": []}, "context_score": 4.1195312, "type_score": 32.273438, "context_posterior": 3.7000002e-05, "name_score": 0.029146001, "prior": 0, "score": 0.0059739999}]}}, "name": "Titanic", "entity_type": "NON", "representative_mention": 0, "entity_type_probability": [], "mention": [{"phrase": {"start": 5, "head": 5, "end": 5}, "kind": 1, "type": 0}], "type": []}], "topic": [], "token": [{"category": "NOUN", "head": 1, "end": 4, "break_level": 0, "label": "nn", "start": 0, "tag": "NNP", "word": "James"}, {"category": "NOUN", "head": 2, "end": 12, "break_level": 1, "label": "nsubj", "start": 6, "tag": "NNP", "word": "Cameron"}, {"category": "VERB", "end": 21, "break_level": 1, "label": "ROOT", "start": 14, "tag": "VBD", "word": "directed"}, {"category": "DET", "head": 4, "end": 25, "break_level": 1, "label": "det", "start": 23, "tag": "DT", "word": "the"}, {"category": "NOUN", "head": 2, "end": 31, "break_level": 1, "label": "dobj", "start": 27, "tag": "NN", "word": "movie"}, {"category": "NOUN", "head": 4, "end": 39, "break_level": 1, "label": "appos", "start": 33, "tag": "NNP", "word": "Titanic"}, {"category": "ADP", "head": 5, "end": 42, "break_level": 1, "label": "prep", "start": 41, "tag": "IN", "word": "in"}, {"category": "NUM", "head": 6, "end": 47, "break_level": 1, "label": "pobj", "start": 44, "tag": "CD", "word": "1997"}], "relation": [], "subsection": [], "measure": [{"info": {"[DateAnnotations]": {"instance": [{"text": "1997", "begin": 44, "end": 48, "clean_text": "1997", "year": 1997}]}}, "phrase": {"start": 7, "end": 7}, "type": 3, "value": "1997"}], "annotations": {"[NumberAnnotations]": {"instance": [{"value_int": 1997, "text": "1997", "begin": 44, "end": 48}]}, "[DateAnnotations]": {"instance": [{"text": "1997", "begin": 44, "end": 48, "clean_text": "1997", "year": 1997}]}}, "semantic_node": [{"phrase": {"start": 2, "end": 2}, "kind": 1, "arc": [{"type": "0", "description": "director", "semantic_node": 1}, {"type": "1", "description": "thing directed", "semantic_node": 2}], "type": "direct.01.V", "description": "provide direction"}, {"phrase": {"start": 0, "end": 1}, "kind": 1, "arc": []}, {"phrase": {"start": 3, "end": 7}, "kind": 1, "arc": []}], "[nlp_saft.GraphParserLambda.extension]":{"dependency_lambda":["((direct.arg_1 2:e 1:n) (direct.arg_2 2:e 5:n) (direct.in.arg2 2:e 7:n) (1997 7:s 7:n) (Cameron 1:s 1:n) (movie 4:s 5:n) (Titanic 5:s 5:n))"]}}'''
  # print line
  # 
  
  cache = set()
  for i, line in enumerate(sys.stdin):
      document = simplejson.loads(line.strip())  
      gdoc = document_to_graphparser_input(document)
      sentence = " ".join([word['word'] for word in gdoc['words']])
      if sentence not in cache:
        print simplejson.dumps(gdoc)
        cache.add(sentence)
