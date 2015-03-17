import sys
import json

# Ideally OTROS means other. Since this isn't available in 7 class MUC labels, we use ORGANIZATION.
ner_mapping = {"LUG": "LOCATION", "PERS": "PERSON", "ORG": "ORGANIZATION", "OTROS": "ORGANIZATION"} 

for line in sys.stdin:
    sentence = json.loads(line)
    if 'entities' in sentence:
        for entity in sentence['entities']:
            if 'ner' in entity:
                entity['ner'] = ner_mapping[entity['ner']]
    if 'words' in sentence:
        del sentence['words']
        if 'spanish' in sentence:
            del sentence['spanish']
        if 'english' in sentence:
            del sentence['english']
        if 'targetValue' in sentence:
            del sentence['targetValue']
        if 'url' in sentence:
            del sentence['url']
    print json.dumps(sentence)
