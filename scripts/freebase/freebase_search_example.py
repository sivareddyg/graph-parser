# -*- coding: utf-8 -*-
import json
import urllib

api_key = "AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg"
query = 'los patos de oregón'
service_url = 'https://www.googleapis.com/freebase/v1/search'
params = {
        'query': query,
        'key': api_key,
        'lang': 'es'
}
url = service_url + '?' + urllib.urlencode(params)
response = json.loads(urllib.urlopen(url).read())
for result in response['result']:
    print result['name'] + ' (' + str(result['score']) + ')'

#print response
