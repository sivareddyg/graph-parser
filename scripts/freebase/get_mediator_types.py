'''
Created on 26 Aug 2013

@author: Siva Reddy
'''

import json
import urllib
import sys

def get_mediator_types(domain, api_key):
    
    service_url = 'https://www.googleapis.com/freebase/v1/mqlread'
    query = [{ "type": "/type/type", "domain": {"id": "/%s" %(domain)}, "/freebase/type_hints/mediator": True, "id": None }]
    params = {
            'query': json.dumps(query),
            'key': api_key
    }
    url = service_url + '?' + urllib.urlencode(params)
    response = json.loads(urllib.urlopen(url).read())
    return [result['id'] for result in response['result']]
    
if __name__ == "__main__":
    domain = sys.argv[1]
    api_key = "AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg"
    print get_mediator_types(domain, api_key)