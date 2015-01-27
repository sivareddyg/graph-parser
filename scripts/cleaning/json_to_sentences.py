import json
import sys


for line in sys.stdin:
    parts = json.loads(line)
    words = [word['word'] for word in parts['words']]
    print " ".join(words).encode('utf-8', 'ignore')
