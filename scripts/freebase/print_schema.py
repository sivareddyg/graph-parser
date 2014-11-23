'''
Created on 26 Aug 2013

@author: Siva Reddy

Requires SPARQLWrapper: http://sparql-wrapper.sourceforge.net/

Return all the types in the domain

'''

from freebase_graph_tools import Freebase
import sys
import re

def rdfize(entity):
    if entity == None:
        return "none"
    return entity.strip("/").replace("/", ".")

def get_schema_using_mql(domain, api_key):
    freebase = Freebase(api_key)
    ftypes = freebase.get_domain_types_namespace(domain)
    
    domain_types = [ftype['id'] for ftype in ftypes]
    domain_types = set(domain_types)
    # print domain_types
    
    special_types_direct = {}
    special_types_mediators = {}
    mediators = {}
    for ftype in ftypes:
        type_id = ftype["id"]
        mediatorType = ftype["/freebase/type_hints/mediator"]
        ftypeType = "main"
        if mediatorType:
            if not mediators.has_key(type_id):
                mediators[type_id] = []
            continue
        print "%s\t%s" %(rdfize(type_id), ftypeType)
        
        fproperties = freebase.get_properties_of_type(type_id)
        for fproperty in fproperties:
            property_id = fproperty["id"]
            property_master = fproperty["/type/property/master_property"]
            property_reverse = fproperty["/type/property/reverse_property"]
            expected_type = fproperty["/type/property/expected_type"]
        
            property_type = "master"
            if property_master != None:
                property_type = "reverse"
                property_reverse = property_master
            
            sys.stderr.write(property_id + '\n')
            if expected_type == None:
                continue;
            sys.stderr.write('\t' + expected_type + '\n');
            
            expected_type_is_mediator = freebase.check_if_a_type_is_mediator(expected_type)
            
            if property_reverse != None:
                print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
            elif not expected_type.startswith("/type/") and not expected_type.startswith("/common/") and expected_type_is_mediator:
                print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_id) + ".inverse")
            else:
                print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
            
            if ftypeType == "main" and not expected_type.startswith("/type/") and not expected_type.startswith("/common/"):
                if expected_type_is_mediator and expected_type not in domain_types:
                    if not special_types_mediators.has_key(expected_type):
                        special_types_mediators[expected_type] = []
                    if property_reverse == None:
                        property_reverse = rdfize(property_id) + ".inverse"
                        property_details = "\t%s\t%s\t%s\t%s" %(rdfize(property_reverse), rdfize(type_id), "reverse", rdfize(property_id))
                        special_types_mediators[expected_type].append(property_details)
                
                elif expected_type_is_mediator and expected_type in domain_types:
                    if not mediators.has_key(expected_type):
                        mediators[expected_type] = []
                    if property_reverse == None:
                        property_reverse = rdfize(property_id) + ".inverse"
                        property_details = "\t%s\t%s\t%s\t%s" %(rdfize(property_reverse), rdfize(type_id), "reverse", rdfize(property_id))
                        mediators[expected_type].append(property_details)
                    
                elif expected_type not in domain_types:
                    if not special_types_direct.has_key(expected_type):
                        special_types_direct[expected_type] = []
                    if property_reverse != None:
                        expected_property_type = "reverse"
                        if property_type == "reverse":
                            expected_property_type = "master"
                        property_details = "\t%s\t%s\t%s\t%s" %(rdfize(property_reverse), rdfize(type_id), expected_property_type, rdfize(property_id)) 
                        special_types_direct[expected_type].append(property_details)
        print
    
    for ftype in mediators:
        print "%s\t%s" %(rdfize(ftype), "mediator")
        type_id = ftype
        fproperties = freebase.get_properties_of_type(type_id)
        for fproperty in mediators[ftype]:
            print fproperty
        for fproperty in fproperties:
            property_id = fproperty["id"]
            property_master = fproperty["/type/property/master_property"]
            property_reverse = fproperty["/type/property/reverse_property"]
            expected_type = fproperty["/type/property/expected_type"]
        
            property_type = "master"
            if property_master != None:
                property_type = "reverse"
                property_reverse = property_master
            
            print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
            if property_id.endswith("_s") or (property_reverse != None and property_reverse.endswith("_s")):
                print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
        print
        
    for ftype in special_types_direct:
        print "%s\t%s" %(rdfize(ftype), "foreign")
        for fproperty in special_types_direct[ftype]:
            print fproperty
        print 
    
    for ftype in special_types_mediators:
        print "%s\t%s" %(rdfize(ftype), "foreign_mediator")
        type_id = ftype
        fproperties = freebase.get_properties_of_type(type_id)
        for fproperty in special_types_mediators[ftype]:
            print fproperty
        for fproperty in fproperties:
            property_id = fproperty["id"]
            property_master = fproperty["/type/property/master_property"]
            property_reverse = fproperty["/type/property/reverse_property"]
            expected_type = fproperty["/type/property/expected_type"]
        
            property_type = "master"
            if property_master != None:
                property_type = "reverse"
                property_reverse = property_master
            print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
            if property_id.endswith("_s") or (property_reverse != None and property_reverse.endswith("_s")):
                print "\t%s\t%s\t%s\t%s" %(rdfize(property_id), rdfize(expected_type), property_type, rdfize(property_reverse))
        print
        

def get_types_using_sparql(sparql_end_point, domain, api_key):
    '''
    sparql_end_point e.g. http://localhost:8890/sparql
    domain e.g. film
    Google API key for freebase e.g. AIzaSyDj-4Sr5TmDuEA8UVOd_89PqK87GABeoFg
    '''
    from SPARQLWrapper import SPARQLWrapper, JSON
    
    mediator_types = {}
    freebase = Freebase(api_key)
    for mediator in freebase.get_mediator_types(domain):
        mediator = "http://rdf.freebase.com/ns/" + mediator.strip("/").replace("/", ".")
        mediator_types[mediator] = 1
        
    sparql = SPARQLWrapper(sparql_end_point)
    query = """prefix ns: <http://rdf.freebase.com/ns/>
        prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        select distinct ?type
        from <http://%s.freebase.com>
        where {
             ?type ns:type.object.type ns:type.type .
             FILTER(regex(?type, "/ns/%s")) .
        }""" %(domain, domain)
    sparql.setQuery(query)
    sparql.setReturnFormat(JSON)
    type_results = sparql.query().convert()
    
    query = """prefix ns: <http://rdf.freebase.com/ns/>
        prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        select distinct ?x ?y
        from <http://%s.freebase.com>
        where {
             ?x ns:type.object.type ns:type.property .
             ?x ns:type.property.reverse_property ?y .
             FILTER(regex(?x, "/ns/%s")) .
        }""" %(domain, domain)
    sparql.setQuery(query)
    results = sparql.query().convert()
    
    master_properties = {}
    reverse_properties = {} 
    for result in results["results"]["bindings"]:
        master_properties[result["x"]["value"]] = result["y"]["value"]
        reverse_properties[result["y"]["value"]] = result["x"]["value"]
    
    query = """prefix ns: <http://rdf.freebase.com/ns/>
        prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        select distinct ?x ?y ?z
        from <http://%s.freebase.com>
        where {
             ?x ns:type.object.type ns:type.property .
             ?x ns:type.property.schema ?y .
             ?x ns:type.property.expected_type ?z . 
             FILTER(regex(?x, "/ns/%s")) .
        }""" %(domain, domain)
    sparql.setQuery(query)
    results = sparql.query().convert()
    
    properties = {}
    for result in results["results"]["bindings"]:
        parent = result["y"]["value"]
        child = result["z"]["value"]
        propertyName = result["x"]["value"]
        if not properties.has_key(parent):
            properties[parent] = []
        properties[parent].append([propertyName, child])
    
    
    for result in type_results["results"]["bindings"]:
        category_type = result["type"]["value"] 
        if mediator_types.has_key(category_type):
            print "%s\t%s" %(category_type, "mediator")
        else:
            print "%s\t%s" %(category_type, "main")
        if properties.has_key(category_type):
            for propertyName, expected_type in properties[category_type]:
                master = "master"
                reverse_property = "none"
                if master_properties.has_key(propertyName):
                    master = "master"
                    reverse_property = master_properties[propertyName]
                elif reverse_properties.has_key(propertyName):
                    master = "inverse"
                    reverse_property = reverse_properties[propertyName]
                print "\t%s\t%s\t%s\t%s" %(propertyName, expected_type, master, reverse_property)
        print

if __name__ == "__main__":
    # sparql_end_point example http://localhost:8890/sparql
    # sparql_end_point = sys.argv[1]
    # domain e.g. film 
    domain = sys.argv[1]
    # Google API key for freebase
    api_key = sys.argv[2]
    # get_types_using_sparql(sparql_end_point, domain, api_key)
    get_schema_using_mql(domain, api_key) 
