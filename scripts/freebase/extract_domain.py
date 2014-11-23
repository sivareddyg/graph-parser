'''
Created on 15 Aug 2013

@author: Siva Reddy
'''

import sys
from collections import Counter
import os

prefixes = '''@prefix ns: <http://rdf.freebase.com/ns/>.
@prefix key: <http://rdf.freebase.com/key/>.
@prefix owl: <http://www.w3.org/2002/07/owl#>.
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>.
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>.
@prefix xsd: <http://www.w3.org/2001/XMLSchema#>.
'''


def extract_domain(schema_dir, entity_dir, relations_dir, domain_dir):
    if not os.path.exists(relations_dir):
        os.mkdir(relations_dir)
    
    if not os.path.exists(domain_dir):
        os.mkdir(domain_dir)
    
    domains_all = set()
    
    # read the schema
    # read the entities
    # if an entity occurs in the domain_name and the relation is in the database, then it is a valid fact
    
    relations2Domain = {} # relation_name : [domain1, domain2, ...]
    schema_files = os.listdir(schema_dir)
    for schema_file in schema_files:
        if schema_file[0] == ".":
            continue
        domain_name = schema_file.split("_")[0]
        domains_all.add(domain_name)
        schema_file = os.path.join(schema_dir, schema_file)
        for line in open(schema_file):
            line = line.rstrip();
            if line == "":
                continue
            if line[0] == '\t':
                line = line.strip();
                parts = line.split('\t')
                rel = parts[0]
                rel_inv = parts[3]
                if not relations2Domain.has_key(rel):
                    relations2Domain[rel] = set()
                if not relations2Domain.has_key(rel_inv):
                    relations2Domain[rel_inv] = set()
                relations2Domain[rel].add(domain_name)
                relations2Domain[rel_inv].add(domain_name)
    
    entities2Domain = {}
    entity_files = os.listdir(entity_dir)
    for entity_file in entity_files:
        if entity_file.find("entities_stats.txt") < 0:
            continue
        domain_name = entity_file.split("_")[0]
        entity_file = os.path.join(entity_dir, entity_file)
        for line in open(entity_file):
            parts = line.split("\t")
            entity = parts[0][5:]
            if not entities2Domain.has_key(entity):
                entities2Domain[entity] = set()
            entities2Domain[entity].add(domain_name)
    
    domains_fp = {}
    relationStats = {};
    for domain_name in domains_all:
        domain_file = os.path.join(domain_dir, domain_name + "-freebase-rdf-2013-08-11-00-00")
        domains_fp[domain_name] = open(domain_file, "w")
        domains_fp[domain_name].write(prefixes);
        relationStats[domain_name] = Counter()
    
    count = 0
    for line in sys.stdin:
        parts = line.strip().strip(".").split('\t')
        if len(parts) == 3 and relations2Domain.has_key(parts[1][3:]) and (entities2Domain.has_key(parts[0][5:]) or entities2Domain.has_key(parts[2][5:])):
                entity1 = parts[0][5:]
                entity2 = parts[2][5:]
                relation_name = parts[1][3:]
                count += 1
                if count % 100000 == 0:
                    sys.stderr.write("Number of facts: %d\n" %count)
                domains = set()
                if entities2Domain.has_key(entity1):
                    domains = domains.union(entities2Domain[entity1]) 
                if entities2Domain.has_key(entity2):
                    domains = domains.union(entities2Domain[entity2])
                domains = domains.intersection(relations2Domain[relation_name])
                line = " ".join(parts) + " ."
                for domain in domains:
                    domains_fp[domain].write(line + '\n')
                    relationStats[domain][relation_name] += 1
    
    for domain_name in domains_all:
        domains_fp[domain_name].close()
    
    for domain_name in domains_all:
        items = relationStats[domain_name].items()
        items.sort(key = lambda x : x[1], reverse = True)
        out_file_name = os.path.join(relations_dir, domain_name + "_relations_stats.txt")
        out_file = open(out_file_name, 'w')
        for relation, freq in items:
            out_file.write("ns:%s\t%d\n" %(relation, freq))
        out_file.close()
    
if __name__ == "__main__":
    extract_domain(sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4])