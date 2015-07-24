'''
Created on 18 Nov 2013

@author: Siva Reddy
'''

from collections import Counter
import os
import sys


def extract_entities(schema_dir, out_dir):
    '''
    extract entities by using the relations that connect domain's main/mediator
    types with any other entity types. To do this, process only the
    relations with which main/mediator types appear.
    '''
    if not os.path.exists(out_dir):
        os.mkdir(out_dir)

    entities = {}  # domain_name : Set()
    schema_files = os.listdir(schema_dir)
    relations2Domain = {}  # relation_name : [domain1, domain2, ...]
    for schema_file in schema_files:
        if schema_file[0] == ".":
            continue
        isDomainEntity = False
        domain_name = schema_file.split("_")[0]
        entities[domain_name] = Counter()
        schema_file = os.path.join(schema_dir, schema_file)
        for line in open(schema_file):
            line = line.rstrip()
            if line == "":
                isDomainEntity = False
                continue
            elif line[0] != "\t":
                parts = line.split('\t')
                rel_type = parts[1]
                if rel_type == "main" or rel_type == "mediator":
                    isDomainEntity = True
            elif line[0] == '\t' and isDomainEntity:
                line = line.strip()
                parts = line.split('\t')
                rel = parts[0]
                rel_inv = parts[3]

                if not relations2Domain.has_key(rel):
                    relations2Domain[rel] = set()
                if not relations2Domain.has_key(rel_inv):
                    relations2Domain[rel_inv] = set()
                relations2Domain[rel].add(domain_name)
                relations2Domain[rel_inv].add(domain_name)

    count = 1
    for line in sys.stdin:
        if count % 100000 == 0:
            print count
        if line[0] == '@':
            # line = line.replace("rdf.freebase.com", "rdf.freebase.com" %(domain_name))
            continue
        else:
            parts = line.strip().split('\t')
            if len(parts) == 3:
                relation_name = parts[1][3:]
                if relations2Domain.has_key(relation_name):
                    count += 1
                    for domain_name in relations2Domain[relation_name]:
                        if parts[0].startswith("ns:m."):
                            entities[domain_name][parts[0]] += 1
                        if parts[2].startswith("ns:m."):
                            entities[domain_name][parts[2].strip(".")] += 1

    for domain_name in entities:
        out_file = open(
            os.path.join(out_dir, domain_name + "_entities_stats.txt"), "w")
        items = entities[domain_name].items()
        items.sort(key=lambda x: x[1], reverse=True)
        for entity, freq in items:
            out_file.write("%s\t%d\n" % (entity, freq))
        out_file.close()

if __name__ == "__main__":
    extract_entities(sys.argv[1], sys.argv[2])
