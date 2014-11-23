import sys
import json

for line in sys.stdin:
    if line[0] == "#" or line[0] == "\n":
        continue
    parts = json.loads(line)
    words = [word['word'] for word in parts['words']]
    print " ".join(words)
