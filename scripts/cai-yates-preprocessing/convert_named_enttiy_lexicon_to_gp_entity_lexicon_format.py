import sys
import json

for line in sys.stdin:
    parts = line.strip().split("\t")
    if parts[1].startswith("m."):
        print "%s\t%s" %(parts[1], parts[0])
    elif parts[1].find("/type/datetime") >= 0:
        print "%s\t%s" %("type.datetime", parts[0])
    elif parts[1].startswith("/un/"):
        print "%s\t%s" %("type.datetime", parts[0])
    elif parts[1].find("/type/int") >= 0:
        print "%s\t%s" %("type.int", parts[0])
    elif parts[1].find("/type/float") >= 0:
        print "%s\t%s" %("type.float", parts[0])
