'''
Created on 3 Sep 2015

@author: siva
'''

import json
import sys

# {'entropy': 2.0567150115966797, 'nl_string': u"'s.arg_2|between.arg_2", \
# 'grounding_stats': [{'probability': 0.27272728085517883, 'kg_string': \
# u'<UNGROUNDED>', 'pmi': -0.11212944984436035}, {'probability': \
# 0.1515151560306549, 'kg_string': u'!/location/location/containedby', 'pmi': \
# 0.16814124584197998}, {'probability': 0.09090909361839294, 'kg_string': \
# u'!/location/location/street_address./location/mailing_address/state_province_region', \
# 'pmi': 0.2504878342151642}, {'probability': 0.06060606241226196,
# 'kg_string': u'!/location/location/street_address./location/mailing_address/country',
# 'pmi': 0.13201014697551727}]}

for line in sys.stdin:
    entry = json.loads(line)
    print entry["nl_string"]
