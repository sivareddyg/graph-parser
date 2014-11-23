import os
import sys

import random

def load_freebase(database_file, domain):
    commands_file = database_file + ".sql"
    f= open(commands_file, 'w')
    # f.write("delete from DB.DBA.LOAD_LIST;\n");
    # f.write("DB.DBA.TTLP_MT (file_to_string_output ('%s'), '', 'http://%s.freebase.com');\n" %(os.path.abspath(database_file), domain));
    f.write("SPARQL CLEAR GRAPH <http://%s.freebase.com>;\n" %(domain))
    f.write("DB.DBA.TTLP_MT (gz_file_open('%s'), '', 'http://%s.freebase.com', 128);\n" %(os.path.abspath(database_file), domain));
    # f.write("ld_dir('/home/siva/SemanticParsing/freebase/freebase_dump/',  '%s' , 'http://rdf.freebase.com.%s');\n" %(commands_file, commands_file[-2:]))
    # f.write("rdf_loader_run();\n");
    f.write("checkpoint;\n")
    f.write("commit WORK;\n")
    f.write("checkpoint;\n")
    f.write("exit;\n")
    f.close()
    print "Loading File %s" %(commands_file)
    #break
    os.system("isql-vt localhost dba dba %s" %(commands_file))
    os.system("rm %s" %(commands_file))

if __name__ == "__main__":
    database_file = sys.argv[1]
    domain = sys.argv[2]
    load_freebase(database_file, domain)