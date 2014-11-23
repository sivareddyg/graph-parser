'''

@author: Siva Reddy
http://sivareddy.in

Removes lines from freeebase which do not adhere to RDF standards.

usage: python clean_freeebase.py < input_dump > output_dump

Best is to use compressed files itself.

zcat input_dump.gz | python clean_freebase.py | gzip > output_dump.gz

'''

#!/usr/bin/env python

import re
import sys

prefixes = re.compile("@")
quotes = re.compile("[\"]")

line_number = 0
for line in sys.stdin:
    line_number += 1
    line = line.rstrip()
    if line == "":
        sys.stdout.write('\n')
    elif prefixes.match(line):
        sys.stdout.write(line + '\n')
    elif line[-1] != ".":
        sys.stderr.write("No full stop: skipping line %d\n" %(line_number))
        continue
    elif len(quotes.findall(line)) % 2 != 0:
        sys.stderr.write("Incomplete quotes: skipping line %d\n" %(line_number))
        continue
    else:
        parts = line.split("\t")
        if len(parts) != 3 or parts[0].strip() == "" or parts[1].strip() == "" or parts[2].strip() == "":
            sys.stderr.write("n tuple size != 3: skipping line %d\n" %(line_number))
            continue
        # line = " ".join(parts)
        # sys.stdout.write(line[:-1] + " .\n")
        sys.stdout.write(line + '\n')
