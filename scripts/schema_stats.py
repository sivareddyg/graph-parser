import sys

types = set()
relations = set()

for line in sys.stdin:
	line = line.rstrip()
	if line == "":
		continue
	if line[0] == "\t":
		rel, arg, reltype, inv_rel = line.strip().split("\t")
		if reltype == "master":
			relations.add(rel)
		types.add(arg)
	else:
		types.add(line.split()[0])

print "#Types", len(types)

print "#Relations", len(relations)
