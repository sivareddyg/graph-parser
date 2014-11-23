'''
Created on 6 Sep 2013

@author: Siva Reddy
'''

import sys
import json
# import os
# from subprocess import Popen, PIPE
import pexpect

command = """./tools/candc_svn/bin/candc \
--models ./tools/candc_svn/models/questions/ \
--candc-parser-noisy_rules=false \
--candc-parser-extra_rules=false \
--candc-printer=ccgbank \
--candc-trans_brackets=true \
--candc-skip_quotes"""

parser = pexpect.spawn(command)

for line in sys.stdin:
    parts = json.loads(line)
    words = [word['word'] for word in parts['words']]
    sent = " ".join(words) + '\n'
    sent = sent.encode('utf-8', 'ignore')
    
    parser.sendline(sent)
    try:
        parser.expect("\n\(.*\)\r\n", timeout = 0)
        parse = parser.after
        parts['synPar'] = parse.strip()
        print json.dumps(parts)
    except pexpect.TIMEOUT:
        continue

parser.terminate()