dump_bow_graphs_%:
	java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/all_domains_schema.txt \
		buck \
		20 \
		< ../udeplambda/data/webquestions/en/en-bilty-bist-webquestions.$*.deplambda.json \
		> working/wq.bow.graphs.$*.json

dump_gq_bow_graphs_%:
	java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/sempre2_schema_gp_format.txt \
		buck \
		20 \
		< ../udeplambda/data/GraphQuestions/en/en-bilty-bist-graphquestions.$*.deplambda.forest.json \
		> working/gq.bow.graphs.$*.json

spades_predicates_%:
	zcat data/distant_eval/parses/$*/dev.json.blank.cleaned.gz \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli  \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping data/distant_eval/semantic_markup_files/indexed_category_mapping_$*.txt \
		-ccgLexicon data/distant_eval/semantic_markup_files/specialcases_$*.txt \
		-ccgLexiconQuestions lib_data/dummy.txt \
		-unaryRules data/distant_eval/semantic_markup_files/unary_rules_$*.txt \
		-binaryRules data/distant_eval/semantic_markup_files/binary_rules_$*.txt \
		| grep ^{ \
		> working/spades.$*.predicates.dev.txt
	zcat data/distant_eval/parses/$*/train.json.blank.cleaned.gz \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli  \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping data/distant_eval/semantic_markup_files/indexed_category_mapping_$*.txt \
		-ccgLexicon data/distant_eval/semantic_markup_files/specialcases_$*.txt \
		-ccgLexiconQuestions lib_data/dummy.txt \
		-unaryRules data/distant_eval/semantic_markup_files/unary_rules_$*.txt \
		-binaryRules data/distant_eval/semantic_markup_files/binary_rules_$*.txt \
		| grep ^{ \
		> working/spades.$*.predicates.train.txt
	zcat data/distant_eval/parses/$*/test.json.blank.cleaned.gz \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli  \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping data/distant_eval/semantic_markup_files/indexed_category_mapping_$*.txt \
		-ccgLexicon data/distant_eval/semantic_markup_files/specialcases_$*.txt \
		-ccgLexiconQuestions lib_data/dummy.txt \
		-unaryRules data/distant_eval/semantic_markup_files/unary_rules_$*.txt \
		-binaryRules data/distant_eval/semantic_markup_files/binary_rules_$*.txt \
		| grep ^{ \
		> working/spades.$*.predicates.test.txt

spades_predicates_%:
	zcat data/distant_eval/parses/$*/dev.json.blank.cleaned.gz \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping data/distant_eval/semantic_markup_files/indexed_category_mapping_$*.txt \
		-ccgLexicon data/distant_eval/semantic_markup_files/specialcases_$*.txt \
		-ccgLexiconQuestions lib_data/dummy.txt \
		-unaryRules data/distant_eval/semantic_markup_files/unary_rules_$*.txt \
		-binaryRules data/distant_eval/semantic_markup_files/binary_rules_$*.txt \
		| grep ^{ \
		> working/spades.$*.predicates.dev.txt

