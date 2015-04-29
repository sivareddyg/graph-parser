
# ns:m.05byxm ns:type.object.key "/en/j_records" .
import sys

for line in sys.stdin:
    if line.find("ns:type.object.key") > 0:
        parts = line.split("\t", 2)
        # print parts
        if parts[0].startswith("ns:m.") and parts[1] == "ns:type.object.key" and parts[2].startswith("\"/en/"):
            print "%s\t%s" % (parts[0][3:], parts[2][5:].strip('\n ."'))