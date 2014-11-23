'''
Created on 30 Aug 2013

@author: Siva Reddy
'''

import hashlib
import sys
import simplejson

sentences_hashes = set()

for line in sys.stdin:
    line = line.rstrip()
    try:
        parts = simplejson.loads(line)
        words = [word['word'] for word in parts['words']]
        sent = " ".join(words).encode('ascii', 'ignore')
        # sys.stderr.write(sent + "\n")
        
        hashkey = hashlib.md5(sent).hexdigest()
        if hashkey in sentences_hashes:
            continue
        else:
            print line
            sentences_hashes.add(hashkey)
            if len(sentences_hashes) > 5000000:
                sentences_hashes = set()
    except:
        sys.stderr.write(line + '\n')