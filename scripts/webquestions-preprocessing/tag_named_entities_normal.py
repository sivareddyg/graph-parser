'''
Created on 20 Sep 2013

@author: Siva Reddy
'''

import sys
import re
import json

named_entity = re.compile("^[a-zA-Z-]")

def load_gazetteer(gazetteer):
    '''
        Gazetteer is stored in the form of a tree. The
        format of loaded gazetteer tree is 
        {
            current_word: {
                        'is_phrase' (if the phrase is valid): True or False,
                        'subtree' {if this phrase is a part of a longer phrase} : {} 
                        'cur_phrase' (current_phrase): the_phrase_that_appeared_so_far: current_phrase
                        'entities' (the list of entities that map this phrase if the phrase is valid): [] 
                        }
        }
    '''
    gazetteer_tree = {}  
    for line in open(gazetteer):
        name, entity = line.strip().split(" :- NP : ")
        # print name, entity
        if entity[-2:] == ":n":
            continue
        tkns = name.split()
        cur_tree = gazetteer_tree
        cur_phrase = ""
        for tkn in tkns[:-1]:
            cur_phrase += tkn 
            if not cur_tree.has_key(tkn):
                cur_tree[tkn] = {'is_phrase': False, 'subtree': {}, 'cur_phrase': cur_phrase, 'entities': []}
            cur_tree = cur_tree[tkn]['subtree']
            cur_phrase += " "
        cur_phrase += tkns[-1] 
        if not cur_tree.has_key(tkns[-1]):
            cur_tree[tkns[-1]] = {'is_phrase': [], 'subtree': {}, 'cur_phrase': cur_phrase, 'entities': []}
        cur_tree[tkns[-1]]['is_phrase'] = True
        if entity not in cur_tree[tkns[-1]]['entities']:
            cur_tree[tkns[-1]]['entities'].append(entity)
    # print gazetteer_tree
    return gazetteer_tree
    

def identify_cur_entity(i, sent, gazetteer_subtree):
    '''
    Example: 
    
    <s>
    The     DT      the
    city    NN      city
    of      PP      of
    New     NP      new
    York    NP      York
    is      VB      is
    in      PP      in
    the     DT      the
    New     NP      new
    York    NP      york
    State   NP      state
    </s>
    
    
    i = 0; entity = The city of New york 
    i = 6; entity = the New York
    
    '''
        
    word = sent[i].lower()
    if gazetteer_subtree.has_key(word):
        if i+1 < len(sent) and gazetteer_subtree[word].has_key('subtree'):
            sub_phrase = identify_cur_entity(i+1, sent, gazetteer_subtree[word]['subtree'])
            if sub_phrase != None:
                return sub_phrase
        #TODO confusing: what if the entities come next to each other
        if gazetteer_subtree[word].has_key('is_phrase') and gazetteer_subtree[word]['is_phrase']:
            final_phrase = {'end': i, 'entities': gazetteer_subtree[word]['entities'], 
                            'phrase': gazetteer_subtree[word]['cur_phrase']}
            return final_phrase
        else:
            return None
    else:
        return None

def identify_entities(sent, gazetteer_tree):
    '''
        An entity is a dictionary having the key words 
        start, end, entities (the list of enitity ids), phrase (the string that is matched)
        
        entities is a list of entity data structures.
    '''
    
    entities = []
    i= 0
    while i < len(sent):
        entity = identify_cur_entity(i, sent, gazetteer_tree)
        if entity != None:
            entity['start'] = i
            entities.append(entity)
            i = entity['end']
        i += 1
    return entities

def extract(sentence, gazetteer_tree):
    sent = [word['word'] for word in sentence['words']]
    if sent != []:
        #print sent
        entities = identify_entities(sent, gazetteer_tree)
        entities = [[entity['entities'], [entity['start'], entity['end']]] for entity in entities]
        # entities = [{'entity':";".join(entity['entities']), 'index_start':entity['start'], 'index_end': entity['end']} for entity in entities]
        # sentence['entities'] = entities
        # print json.dumps(sentence)
        
        grouped_elements = []
        grouped_entities = []
        grouped_sent = []
        count_grouped = 0
        cur_word = 0
        for entity in entities:
            for i in range(cur_word, entity[1][0]):
                grouped_sent.append(sent[i])
            grouped_word = []
            for i in range(entity[1][0], entity[1][1]+1):
              grouped_word.append(sent[i][0].upper() + sent[i][1:])
            grouped_word_all = "_".join(grouped_word)
            #if re.match("[^a-z]", grouped_word_all) and len(grouped_word) > 1:
            #    grouped_word_all = "The_" + grouped_word_all
            grouped_word_all = grouped_word_all[0].upper() + grouped_word_all[1:] 
            grouped_sent.append(grouped_word_all)
            cur_word = entity[1][1] + 1
            grouped_entities.append([entity[0], entity[1][0] - count_grouped])
            count_grouped += len(grouped_word) - 1
        for i in range(cur_word, len(sent)):
            grouped_sent.append(sent[i])
    
        entities = [{'entity':";".join(entity[0]), 'index':entity[1]} for entity in grouped_entities] 
        # grouped_elements.append(grouped_entities)
        words = [{"word":word} for word in grouped_sent]
        sentence['entities'] = entities
        sentence['words'] = words
        print json.dumps(sentence)

if __name__== "__main__":
    gazetteer = sys.argv[1]
    #open(sys.argv[2])
    gazetteer_tree = load_gazetteer(gazetteer)
    for line in sys.stdin:
        sentence = json.loads(line)
        extract(sentence, gazetteer_tree)
