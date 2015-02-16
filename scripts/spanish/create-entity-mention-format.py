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
    print json.dumps(sentence)
