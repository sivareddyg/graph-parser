dump_bow_graphs_%:
	java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/all_domains_schema.txt \
		buck \
		20 \
		< ../udeplambda/data/webquestions/en/en-bilty-bist-webquestions.$*.deplambda.json \
		> working/wq.bow.graphs.$*.json

dump_corrected_bow_graphs_%:
	java -Xmx100g -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/all_domains_schema.txt \
		buck \
		20 \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.$*.pass3.deplambda.singletype.json.txt \
		> working/wq.corrected.bow.graphs.$*.json

dump_gq_bow_graphs_%:
	java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/sempre2_schema_gp_format.txt \
		buck \
		20 \
		< ../udeplambda/data/GraphQuestions/en/en-bilty-bist-graphquestions.$*.deplambda.forest.json \
		> working/gq.bow.graphs.$*.json

dump_spades_bow_graphs_%:
	zcat data/spades/$*.json.gz \
		| java -cp bin:lib/* in.sivareddy.scripts.DumpBoWGroundedGraphs \
		data/freebase/schema/business_film_people_schema.txt \
		buck \
		20 \
		| gzip \
		> working/spades.bow.graphs.$*.json.gz

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

wq_predicates_%:
	cat ../udeplambda/working/en-webquestions.$*.easyccg.json \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli  \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping lib_data/candc_markedup.modified \
		-ccgLexicon lib_data/lexicon_specialCases_questions_vanilla.txt \
		-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
		-unaryRules lib_data/unary_rules.txt \
		-binaryRules lib_data/binary_rules.txt \
		| grep ^{ \
		> working/wq.$*.predicates.dev.txt

gq_predicates_%:
	cat ../udeplambda/working/en-graphquestions.$*.easyccg.json \
		| java -cp bin:lib/* in.sivareddy.graphparser.cli.RunDumpPredicatesCli  \
		-endpoint buck \
		-schema data/dummy.txt \
		-relationTypesFile data/dummy.txt \
		-lexicon data/dummy.txt \
		-nthreads 10 \
		-ccgIndexedMapping lib_data/candc_markedup.modified \
		-ccgLexicon lib_data/lexicon_specialCases_questions_vanilla.txt \
		-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
		-unaryRules lib_data/unary_rules.txt \
		-binaryRules lib_data/binary_rules.txt \
		| grep ^{ \
		> working/gq.$*.predicates.dev.txt


hello:
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

