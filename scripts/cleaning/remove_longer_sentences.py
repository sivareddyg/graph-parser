import sys
import json

for line in sys.stdin:
    sentence = json.loads(line)
    if len(sentence['words']) > 30:
        continue
    if len(sentence['entities']) > 5:
        continue
    sys.stdout.write(line)
