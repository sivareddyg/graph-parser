dump_bow_graphs_%:
	java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/all_domains_schema.txt \
		buck \
		20 \
		< ../udeplambda/data/webquestions/en/en-bilty-bist-webquestions.$*.deplambda.json \
		> working/wq.bow.graphs.$*.json
