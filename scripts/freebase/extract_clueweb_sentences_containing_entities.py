'''
Created on 20 Aug 2013

@author: Siva Reddy

Requires warc-clueweb to parse WARC files https://github.com/cdegroc/warc-clueweb
Requires html2text https://github.com/aaronsw/html2text

'''

import warc
import sys
import os
import re
import json

sentence_pattern = re.compile("[\.\?\!]")
alphabets = re.compile("[a-zA-Z\-\,\s\'0-9\.\?\!\_]")

def get_sentence(current_record_content, start_offset, end_offset):
    sent_start = start_offset
    while sent_start > 0:
        if not alphabets.match(current_record_content[sent_start]):
            return [-1, -1]
        if sentence_pattern.match(current_record_content[sent_start]):
            sent_start += 1
            break
        sent_start = sent_start - 1
    
    content_size = len(current_record_content)
    sent_end = end_offset
    while sent_end < content_size:
        if not alphabets.match(current_record_content[sent_end]):
            return [-1, -1]
        if sentence_pattern.match(current_record_content[sent_end]):
            sent_end += 1
            break
        sent_end = sent_end + 1
    return [sent_start, sent_end] 

def extract_sentences(clueweb_directory, freebase_directory, valid_entity_files):
    valid_entities = {}
    for entity_file in valid_entity_files: 
        for line in open(entity_file):
            entity = line.split()[0].strip(".").replace("ns:", "/").replace(".", "/")
            # print entity
            valid_entities[entity] = 1

    sub_directories = os.listdir(freebase_directory) 
    for directory in sub_directories:
        clueweb_subdir = os.path.join(clueweb_directory, directory)
        if not os.path.isdir(os.path.join(clueweb_subdir)):
            sys.stderr.write("%s is not a directory\n" %(directory))
            continue
        freebase_subdir = os.path.join(freebase_directory, directory)
        if not os.path.isdir(os.path.join(freebase_subdir)):
            sys.stderr.write("%s is not a directory\n" %(directory))
            continue
        leaf_files = os.listdir(freebase_subdir)
        for leaf_file in leaf_files:
            warc_file = os.path.join(clueweb_subdir, leaf_file.replace(".anns.tsv", ".warc.gz"))
            annotated_file = os.path.join(freebase_subdir, leaf_file)
            if not os.path.exists(warc_file):
                sys.stderr.write("Skipped: warc file does not exist: %s" % (warc_file))
                continue
            if not os.path.exists(annotated_file):
                sys.stderr.write("Skipped: annotated file does not exist: %s" % (annotated_file))
                continue
            
            warc_file_reader = warc.open(warc_file).__iter__()
            # records = [record for record in warc_file_reader]
            annotated_file_reader = open(annotated_file)
            current_record = None
            current_record_count = -2
            current_record_content = ""
            start_sentence = 0
            end_sentence = 0
            sys.stderr.write("%s %s\n" % (warc_file, annotated_file))
            entities_and_sentence = []
            for line in annotated_file_reader:
                line = line.rstrip()
                columns = line.split('\t')
                entity_tag = columns[-1]
                if valid_entities.has_key(entity_tag):
                    record_number = int(columns[0].split("-")[-1])
                    encoding = columns[1].lower()
                    entity_name = columns[2]
                    start_offset = int(columns[3])
                    end_offset = int(columns[4])
                    score = float(columns[5])
                    # Get the current record
                    while current_record_count < record_number:
                        try:
                            current_record = warc_file_reader.next()
                        except:
                            current_record_count = -100
                            break
                        current_record_count += 1
                        if current_record_count == record_number:
                            try:
                                current_record_content = current_record.payload.decode(encoding, 'replace').encode('utf-8', 'replace')
                            except:
                                current_record_count = -100
                                break
                    if current_record_count < 0:
                        break;
                    if current_record_content[start_offset:end_offset] != entity_name:
                        continue
                    '''if len(records) <= record_number + 1:
                        break
                    current_record_content = records[record_number + 1].payload.decode(encoding, 'replace').encode('utf-8', 'replace')'''
                    # Get the current sentence
                    if start_sentence <= start_offset and end_sentence >= end_offset:
                        entities_and_sentence.append([entity_tag, score, start_offset - start_sentence, end_offset - start_sentence])
                    else:
                        if entities_and_sentence != []:
                            print json.dumps(entities_and_sentence)
                            entities_and_sentence = []
                        [start_sentence, end_sentence] = get_sentence(current_record_content, start_offset, end_offset)
                        if start_sentence != -1:
                            entities_and_sentence.append(current_record_content[start_sentence:end_sentence])
                            entities_and_sentence.append([entity_tag, score, start_offset - start_sentence, end_offset - start_sentence])
                            # print columns[2], "---------------->", current_record_content[start_sentence:end_sentence]
                    # exit(1)
        
        
if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.stderr.write("Takes at least 3 arguments: clueweb directory, freebase annotated directory and files containing entities e.g. /path_clueweb/ClueWeb09_English_1 /path_freebase/ClueWeb09_English_1 /path_entities/film_entities /path_entities/business_entities\n")
        exit(1)

    clueweb_directory = sys.argv[1]
    freebase_directory = sys.argv[2]
    valid_entity_files = sys.argv[3:]
    extract_sentences(clueweb_directory, freebase_directory, valid_entity_files)
    
