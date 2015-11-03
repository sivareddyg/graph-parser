import json
import sys

for line in sys.stdin:
    sent = json.loads(line)
    sent['sparqlQuery'] = sent['targetOrigSparql'].replace("9999", "30")
    del sent['targetOrigSparql']
    del sent['targetSparql']

    # Do not handle sentences which do not have answers.
    if sent['result'] == [] or sent['result'] == ["0"]:
        sent['sparqlQuery'] = "select distinct ?x where {?x ?y ?z} LIMIT 0"
    del sent['result']
    print json.dumps(sent)
