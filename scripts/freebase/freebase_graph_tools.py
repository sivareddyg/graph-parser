'''
Created on 26 Aug 2013

@author: Siva Reddy
'''

import json
import urllib
import sys

class Freebase:
    
    def __init__(self, api_key):
        self.api_key = api_key 

    def run_mql_query(self, query):
        api_key = self.api_key
        service_url = 'https://www.googleapis.com/freebase/v1/mqlread'
        # query = json.loads(query)
        params = {
                'query': query,
                'key': api_key,
                "limit": 10000
        }
        url = service_url + '?' + urllib.urlencode(params)
        response = json.loads(urllib.urlopen(url).read())
        return response['result']
    
    
    def get_domain_types(self, domain):
        query = """[{
          "type": "/type/type",
          "domain": "/%s",
          "id": null,
          "/freebase/type_hints/mediator": null,
          "limit": 10000
        }]""" %(domain)
        response = self.run_mql_query(query)
        return response
    
    def get_domain_types_namespace(self, domain):
        query = """[{
          "type": "/type/type",
          "id": null,
          "key": [{
            "namespace": "/%s"
            }],
          "/freebase/type_hints/mediator": null, 
          "limit": 10000
        }]""" %(domain)
        response = self.run_mql_query(query)
        return response
    
    
    def get_mediator_types(self, domain):
        query = """[{
          "type": "/type/type",
          "domain": "/%s",
          "/freebase/type_hints/mediator": true,
          "id": null,
          "limit": 10000
        }]""" %(domain)
        response = self.run_mql_query(query)
        return response
    
    def get_properties_of_type(self, typeId):
        query = """
        [{
          "type": "/type/property",
          "/type/property/master_property": null,
          "/type/property/reverse_property": null,
          "/type/property/expected_type": null,
          "id": null,
          "/type/property/schema": "%s",
          "limit": 10000
        }] """ %(typeId)
        response = self.run_mql_query(query)
        return response
    
    def check_if_a_type_is_mediator(self, typeId):
        query = """{
          "type": "/type/type",
          "/freebase/type_hints/mediator": null,
          "id": "%s",
          "limit": 10000
        }""" %(typeId)
        result = self.run_mql_query(query)
        return result["/freebase/type_hints/mediator"]
    
    def get_inverse_relation(self, relationId):
        query = """
        {
          "type": "/type/property",
          "/type/property/reverse_property": null,
          "id": "%s",
          "limit": 10000
        } """ %(relationId)
        response = self.run_mql_query(query)
        return response["/type/property/reverse_property"]

    def get_master_relation(self, relationId):
        query = """
        {
          "type": "/type/property",
          "/type/property/master_property": null,
          "id": "%s",
          "limit": 10000
        } """ %(relationId)
        response = self.run_mql_query(query)
        return response["/type/property/master_property"]
    
if __name__ == "__main__":
    domain = "business"
    api_key = "AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg"
    freebase = Freebase(api_key)
    print freebase.get_domain_types(domain)
    print freebase.get_mediator_types(domain)
    print freebase.get_properties_of_type("/business/employment_tenure")
    print freebase.check_if_a_type_is_mediator("/business/employment_tenure")
    print freebase.get_inverse_relation("/business/employment_tenure/person")
    print freebase.get_master_relation("/people/person/employment_history")
    print freebase.check_if_a_type_is_mediator("/award/award_honor")
    print freebase.get_domain_types_namespace(domain)
    
