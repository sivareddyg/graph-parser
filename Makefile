# Merge schema
all_domains_schema:
	python scripts/freebase/merge_schema_folder.py data/freebase/schema/all-domains/ > data/freebase/schema/all_domains_schema.txt

# English Automatic Entity Annotation
#
dump_sempre_db:
	isql-vt localhost:1111 dba dba < scripts/dump_freebase.isql

create_mid_to_key_dict:
	zcat ../data/freebase-cleaned.rdf-2013-08-11-00-00.gz | python scripts/entity-annotation/print_entity_to_id.py | gzip > data/freebase/mid_to_key.txt.gz

convert_sempre_to_standard_db:
	zcat /disk/scratch/users/s1051585/tools/var/lib/virtuoso/vdb/data_000001.ttl.gz \
	| python scripts/entity-annotation/convert_sempre_freebase_to_standard_freebase.py data/freebase/mid_to_key.txt.gz \
	| gzip > /disk/scratch/users/s1051585/data/freebase_sempre.ttl.gz


extract_sempre_entities_list:
	zcat /disk/scratch/users/s1051585/data/freebase_sempre.ttl.gz \
	| python scripts/entity-annotation/extract_entities_from_standard_sempre.py \
	| gzip > data/freebase/freebase_sempre_entities.gz

create_entity_dict:
	zcat /disk/scratch/users/s1051585/data/freebase-cleaned.rdf-2013-08-11-00-00.gz | python scripts/entity-annotation/extract_entities_based_on_langcode.py en,en-gb \
	| java -cp lib/*:bin others.EnglishEntityTokenizer \
	| sed -e 's/\-LRB\-/\(/g' \
	| sed -e 's/\-RRB\-/\)/g' \
	| sed -e 's/\-LSB\-/\[/g' \
	| sed -e 's/\-RSB\-/\]/g' \
	| gzip >  data/freebase/en_entity_lexicon_tokenized.gz

tokenize_entity_dict:
	zcat data/freebase/en_entity_lexicon.gz \
	| java -cp lib/*:bin others.EnglishEntityTokenizer \
	| sed -e 's/\-LRB\-/\(/g' \
	| sed -e 's/\-RRB\-/\)/g' \
	| sed -e 's/\-LSB\-/\[/g' \
	| sed -e 's/\-RSB\-/\]/g' \
	| gzip \
	> data/freebase/en_entity_lexicon_tokenized.gz

entity_tag_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.EntityAnnotator data/freebase/en_entity_lexicon_tokenized.gz \
		> data/webquestions/webquestions.examples.train.domains.entity.matches.json
	cat data/webquestions/webquestions.examples.test.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.EntityAnnotator data/freebase/en_entity_lexicon_tokenized.gz \
		> data/webquestions/webquestions.examples.test.domains.entity.matches.json

rank_entity_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json

select_one_best_from_ranked_entity_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.DisambiguateEntities \
	> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.json
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.DisambiguateEntities \
	> data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.json

merge_one_best:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
  	> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.json	
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
  	> data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.json	

extract_tacl_subset_one_best:
	mkdir -p data/tacl/vanilla_one_best/
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.json \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json \
		| java -cp lib/*:bin others.RunEasyCCGJsonSentence \
		> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json
	head -n915 data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json \
		> data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915
	tail -n200 data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json \
		> data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.200
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.json \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json \
		| java -cp lib/*:bin others.RunEasyCCGJsonSentence \
		> data/tacl/vanilla_one_best/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.tacl.json

one_best_deplambda_format:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.json \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> ../working/webquestions.vanilla.1best.train.full.json.txt
	python scripts/webquestions-preprocessing/training_split.py ../working/webquestions.vanilla.1best.train.full.json.txt 
	mv ../working/webquestions.vanilla.1best.train.full.json.txt.80 ../working/webquestions.vanilla.1best.train.split.json.txt
	mv ../working/webquestions.vanilla.1best.train.full.json.txt.20 ../working/webquestions.vanilla.1best.dev.split.json.txt
	rm ../working/webquestions.vanilla.1best.train.full.json.txt
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.json \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> ../working/webquestions.vanilla.1best.test.full.json.txt
	cat data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915 \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> ../working/webquestions.vanilla.1best.train.business_film_people.json.txt
	cat data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.200 \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> ../working/webquestions.vanilla.1best.dev.business_film_people.json.txt
	cat data/tacl/vanilla_one_best/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.tacl.json \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		> ../working/webquestions.vanilla.1best.test.business_film_people.json.txt

# Convert GraphParser format to deplambda input format
convert_graphparser_to_deplambda_format:
	head -n915 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.train.txt
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.dev.txt
	cat data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.test.txt
	head -n915 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.train.noheuristics.txt
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.dev.noheuristics.txt
	cat data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.noheuristics.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.test.noheuristics.txt
	cat data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people.txt

convert_graphparser_to_deplambda_format_tom:
	cat data/webquestions/webquestions.train.all.entity_annotated.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.train.json.txt
	cat data/webquestions/webquestions.test.all.entity_annotated.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.test.json.txt
	cat data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt | java -cp lib/*:bin in.sivareddy.graphparser.util.AddAnswerMids | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/tom_free917.txt


convert_wq_vanilla_to_deplambda_format:
	mkdir -p data/complete/vanilla_gold/
	python scripts/webquestions-preprocessing/training_split.py ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt 
	cat ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.80 | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt
	cat ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.20 | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt
	cat ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt
	cat ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.vanilla.train.full.json.txt
	python scripts/webquestions-preprocessing/training_split.py ../working/webquestions.vanilla.train.full.json.txt 
	mv ../working/webquestions.vanilla.train.full.json.txt.80 ../working/webquestions.vanilla.train.split.json.txt
	mv ../working/webquestions.vanilla.train.full.json.txt.20 ../working/webquestions.vanilla.dev.split.json.txt
	rm ../working/webquestions.vanilla.train.full.json.txt
	cat ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.vanilla.test.full.json.txt
	python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json < ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt > data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json
	python scripts/extract_subset.py data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json < ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt > data/webquestions/webquestions.examples.test.domains.filtered.vanilla.json
	head -n915 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.vanilla.train.business_film_people.json.txt
	tail -n200 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.vanilla.dev.business_film_people.json.txt
	cat data/webquestions/webquestions.examples.test.domains.filtered.vanilla.json | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/webquestions.vanilla.test.business_film_people.json.txt
	head -n915 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915
	tail -n200 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200
	cat data/webquestions/webquestions.examples.test.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCGJsonSentence > data/tacl/vanilla_gold/webquestions.examples.test.domains.easyccg.parse.filtered.json

convert_deplambda_to_wq_format_vanilla:
	cat data/deplambda/webquestions.vanilla.train.lambdas.txt \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
	> data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt
	cat data/deplambda/webquestions.vanilla.dev.lambdas.txt \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
	> data/complete/vanilla_gold/webquestions.vanilla.dev.full.deplambda.json.txt
	cat data/deplambda/webquestions.vanilla.train.business_film_people.lambdas.txt \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	> data/tacl/vanilla_gold/webquestions.examples.train.domains.deplambda.parse.filtered.json
	cat data/deplambda/webquestions.vanilla.dev.business_film_people.lambdas.txt \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	> data/tacl/vanilla_gold/webquestions.examples.dev.domains.deplambda.parse.filtered.json

convert_cai_yates_splits_to_deplambda:
	mkdir -p ../working/free917_business_film_people_splits
	cat data/cai-yates-2013/free917_business_film_people_splits/0.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/0.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/1.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/1.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/2.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/2.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/3.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/3.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/4.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/4.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/5.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/5.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/6.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/6.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/7.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/7.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/8.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/8.txt
	cat data/cai-yates-2013/free917_business_film_people_splits/9.txt | python  scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > ../working/free917_business_film_people_splits/9.txt

# Converts deplambda documents in json format to graphparsers json format.
convert_deplambda_output_to_graphparser:
	#cat ../data/clueweb-training-documents/* \
	#| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	#| python scripts/cleaning/remove_duplicate_sentences.py \
	#| gzip > data/deplambda/unsupervised.graphparser.txt.gz
	cat data/deplambda/wq-train-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	> data/deplambda/webquestions.train.graphparser.txt
	cat data/deplambda/wq-dev-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	> data/deplambda/webquestions.dev.graphparser.txt
	cat data/deplambda/wq-test-documents.json \
	| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	| python scripts/dependency_semantic_parser/add_answers.py data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	> data/deplambda/webquestions.test.graphparser.txt
	#cat data/deplambda/free917-documents.json \
	#| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py \
	#| python scripts/dependency_semantic_parser/add_answers.py data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt \
	#> data/deplambda/free917.txt

# Create dependency based grounded lexicon.
# Unfortunately, this cannot run parallel version since I have to write
# sentences into a single file.
create_deplambda_grounded_lexicon:
	mkdir -p data/deplambda/sentences_training
	mkdir -p data/deplambda/grounded_lexicon
	zcat data/deplambda/unsupervised.graphparser.txt.gz \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey dependency_lambda \
	--argumentLexicalIdentifiers mid \
	--relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.1.txt \
	| python scripts/freebase-training/filter_training_sentences.py 0 0 0 0 0 \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| gzip > data/deplambda/sentences_training/deplambda_training_sentences.1.txt.gz

# TACL CCG lexicon and training sentences
create_tacl_ccg_grounded_lexicon_and_training_sentences:
	mkdir -p data/tacl/sentences_training
	mkdir -p data/tacl/grounded_lexicon
	zcat data/freebase/sentences_filtered/* \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/candc_markedup.modified \
	--unaryRulesFile lib_data/unary_rules.txt \
	--binaryRulesFile lib_data/binary_rules.txt \
	--specialCasesFile lib_data/lexicon_specialCases.txt \
	--relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	| python scripts/freebase-training/filter_training_sentences.py 0 0 0 0 0 \
	| python scripts/cleaning/remove_duplicate_sentences.py \
	| gzip > data/tacl/sentences_training/tacl_training_sentences.txt.gz

# Deplambda Experiments
# Baseline to evaluate the accuracy of lexicon
deplambda_mwg:
	mkdir -p ../working/deplambda_mwg
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_mwg/business_film_people.log.txt \
	> ../working/deplambda_mwg/business_film_people.txt

deplambda_mwg_dev:
	mkdir -p ../working/deplambda_mwg_dev
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-logFile ../working/deplambda_mwg_dev/business_film_people.log.txt \
	> ../working/deplambda_mwg_dev/business_film_people.txt

deplambda_mwg_train:
	mkdir -p ../working/deplambda_mwg_train
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/deplambda/webquestions.train.graphparser.txt \
	-logFile ../working/deplambda_mwg_train/business_film_people.log.txt \
	> ../working/deplambda_mwg_train/business_film_people.txt

deplambda_mwg_free917:
	mkdir -p ../working/deplambda_mwg_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/deplambda/free917.txt \
	-logFile ../working/deplambda_mwg_free917/business_film_people.log.txt \
	> ../working/deplambda_mwg_free917/business_film_people.txt

# Supervised Expermients
# Deplambda results without unsupervised lexicon.
deplambda_supervised:
	mkdir -p ../working/deplambda_supervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/deplambda/webquestions.train.graphparser.txt \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_supervised/business_film_people.log.txt \
	> ../working/deplambda_supervised/business_film_people.txt

deplambda_supervised_vanilla_gold:
	rm -rf ../working/deplambda_supervised_vanilla_gold
	mkdir -p ../working/deplambda_supervised_vanilla_gold
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 2.0 \
	-endpoint localhost \
	-supervisedCorpus  data/tacl/vanilla_gold/webquestions.examples.train.domains.deplambda.parse.filtered.json \
	-devFile data/tacl/vanilla_gold/webquestions.examples.dev.domains.deplambda.parse.filtered.json \
	-testFile data/tacl/vanilla_gold/webquestions.examples.train.domains.deplambda.parse.filtered.json \
	-logFile ../working/deplambda_supervised_vanilla_gold/all.log.txt \
	> ../working/deplambda_supervised_vanilla_gold/all.txt

deplambda_supervised_vanilla_gold_full:
	rm -rf ../working/deplambda_supervised_vanilla_gold_full
	mkdir -p ../working/deplambda_supervised_vanilla_gold_full
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 2.0 \
	-endpoint localhost \
	-supervisedCorpus  data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-devFile data/complete/vanilla_gold/webquestions.vanilla.dev.full.deplambda.json.txt \
	-testFile data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-logFile ../working/deplambda_supervised_vanilla_gold_full/all.log.txt \
	> ../working/deplambda_supervised_vanilla_gold_full/all.txt

# Deplambda results with unsupervised lexicon.
deplambda_supervised_with_unsupervised_lexicon:
	mkdir -p ../working/deplambda_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/deplambda/webquestions.train.graphparser.txt \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.log.txt \
	> ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.txt

deplambda_supervised_with_unsupervised_lexicon_loaded_model_dev:
	mkdir -p ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_dev
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-loadModelFromFile ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.log.txt.model.bestIteration \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-logFile ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_dev/business_film_people.log.txt \
	> ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_dev/business_film_people.txt

deplambda_supervised_with_unsupervised_lexicon_loaded_model_train:
	mkdir -p ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_train
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-loadModelFromFile ../working/deplambda_supervised_with_unsupervised_lexicon/business_film_people.log.txt.model.bestIteration \
	-devFile data/deplambda/webquestions.train.graphparser.txt \
	-logFile ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_train/business_film_people.log.txt \
	> ../working/deplambda_supervised_with_unsupervised_lexicon_loaded_model_train/business_film_people.txt

# deplambda with unsupervised training
deplambda_unsupervised:
	mkdir -p ../working/deplambda_unsupervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/deplambda/sentences_training/deplambda_training_sentences.txt.gz \
	-devFile data/deplambda/webquestions.dev.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile ../working/deplambda_unsupervised/business_film_people.log.txt \
	> ../working/deplambda_unsupervised/business_film_people.txt

deplambda_unsupervised_free917:
	mkdir -p ../working/deplambda_unsupervised_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-semanticParseKey dependency_lambda \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/deplambda/grounded_lexicon/deplambda_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/deplambda/sentences_training/deplambda_training_sentences.txt.gz \
	-devFile data/deplambda/free917.txt \
	-logFile ../working/deplambda_unsupervised_free917/business_film_people.log.txt \
	> ../working/deplambda_unsupervised_free917/business_film_people.txt

############################################### TACL Experiments  ################################################
# TACL MWG Baseline
tacl_mwg:
	mkdir -p ../working/tacl_mwg
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_mwg/business_film_people.log.txt \
	> ../working/tacl_mwg/business_film_people.txt

tacl_mwg_on_training_data:
	mkdir -p ../working/tacl_mwg_on_training
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-logFile ../working/tacl_mwg_on_training/business_film_people.log.txt \
	> ../working/tacl_mwg_on_training/business_film_people.txt

tacl_mwg_on_training_data_vanilla_gold:
	rm -rf ../working/tacl_mwg_on_training_data_vanilla_gold
	mkdir -p ../working/tacl_mwg_on_training_data_vanilla_gold
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-endpoint localhost \
	-nthreads 10 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-devFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-logFile ../working/tacl_mwg_on_training_data_vanilla_gold/business_film_people.log.txt \
	> ../working/tacl_mwg_on_training_data_vanilla_gold/business_film_people.txt

tacl_mwg_on_training_data_vanilla_gold_onlinekb:
	rm -rf ../working/tacl_mwg_on_training_data_vanilla_gold_onlinekb
	mkdir -p ../working/tacl_mwg_on_training_data_vanilla_gold_onlinekb
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-endpoint localhost \
	-nthreads 50 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-devFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-logFile ../working/tacl_mwg_on_training_data_vanilla_gold_onlinekb/business_film_people.log.txt \
	> ../working/tacl_mwg_on_training_data_vanilla_gold_onlinekb/business_film_people.txt

tacl_mwg_on_training_data_vanilla_one_best:
	mkdir -p ../working/tacl_mwg_on_training_data_vanilla_one_best
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915 \
	-logFile ../working/tacl_mwg_on_training_data_vanilla_one_best/business_film_people.log.txt \
	> ../working/tacl_mwg_on_training_data_vanilla_one_best/business_film_people.txt

tacl_mwg_free917:
	mkdir -p ../working/tacl_mwg_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 10 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag true \
	-argGrelPartFlag true \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType false \
	-validQueryFlag false \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt \
	-logFile ../working/tacl_mwg_free917/business_film_people.log.txt \
	> ../working/tacl_mwg_free917/business_film_people.txt

# TACL GraphPaser results
tacl_unsupervised:
	mkdir -p ../working/tacl_unsupervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/tacl/sentences_training/tacl_training_sentences.txt.gz \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_unsupervised/business_film_people.log.txt \
	> ../working/tacl_unsupervised/business_film_people.txt

tacl_unsupervised_free917:
	mkdir -p ../working/tacl_unsupervised_free917
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 80 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/tacl/sentences_training/tacl_training_sentences.txt.gz \
	-devFile data/cai-yates-2013/question-and-logical-form-917/acl2014_domains/business_film_people_parse.txt \
	-logFile ../working/tacl_unsupervised_free917/business_film_people.log.txt \
	> ../working/tacl_unsupervised_free917/business_film_people.txt

tacl_supervised:
	mkdir -p ../working/tacl_supervised
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised/business_film_people.log.txt \
	> ../working/tacl_supervised/business_film_people.txt

tacl_supervised_vanilla_gold:
	rm -rf ../working/tacl_supervised_vanilla_gold
	mkdir -p ../working/tacl_supervised_vanilla_gold
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-logFile ../working/tacl_supervised_vanilla_gold/business_film_people.log.txt \
	> ../working/tacl_supervised_vanilla_gold/business_film_people.txt

tacl_supervised_vanilla_gold_online_kb:
	rm -rf ../working/tacl_supervised_vanilla_gold_online_kb
	mkdir -p ../working/tacl_supervised_vanilla_gold_online_kb
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-logFile ../working/tacl_supervised_vanilla_gold_online_kb/business_film_people.log.txt \
	> ../working/tacl_supervised_vanilla_gold_online_kb/business_film_people.txt

tacl_supervised_vanilla_gold_full:
	rm -rf ../working/tacl_supervised_vanilla_gold_full
	mkdir -p ../working/tacl_supervised_vanilla_gold_full
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 4000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 2.0 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
	-devFile data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
	-testFile data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
	-logFile ../working/tacl_supervised_vanilla_gold_full/all.log.txt \
	> ../working/tacl_supervised_vanilla_gold_full/all.txt


tacl_supervised_with_unsupervised_lexicon:
	mkdir -p ../working/tacl_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.log.txt \
	> ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.txt

tacl_supervised_with_unsupervised_lexicon_vanilla_gold_online:
	rm -rf ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold
	mkdir -p ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold
	java -Xms2048m -cp lib/*:lib/apache-jena/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	--ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/vanilla_gold/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold/business_film_people.log.txt \
	> ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold/business_film_people.txt

# To load an existing model and to parse an input corpus using it.
tacl_unsupervised_loaded_model:
	mkdir -p ../working/tacl_unsupervised_loaded_model
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 10 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag true \
	-mediatorStemGrelPartMatchingFlag true \
	-argumentStemMatchingFlag true \
	-argumentStemGrelPartMatchingFlag true \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-loadModelFromFile ../working/tacl_unsupervised/business_film_people.log.txt.model.bestIteration \
	-groundInputCorpora data/tacl/sentences_training/business_tacl_training_sentences_small.txt.gz \
	-logFile ../working/tacl_unsupervised_loaded_model/business_film_people.log.txt \
	> ../working/tacl_unsupervised_loaded_model/business_film_people.txt

# Spanish Experiments

# Tokenize Entities
tokenize_spanish_entities:
	zcat data/freebase/spanish/spanish_business_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_business_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_film_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_film_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_people_entities.txt.gz | java -cp lib/*:graph-parser.jar others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_people_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_business_entities.tokenized.txt.gz data/freebase/spanish/spanish_film_entities.tokenized.txt.gz data/freebase/spanish/spanish_people_entities.tokenized.txt.gz | gzip > data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz

extract_spanish_sentences:
	bzcat data/bravas/extracted/AA/wiki_00.bz2 \
		| java -cp lib/*:graph-parser.jar others.SpanishTokenizer                 \
		| perl -pe 's|=LRB=.*?=RRB=||g'                 \
		| grep -v =LRB= | grep -v =RRB=                 \
		| python scripts/spanish/select_sentences_with_entities_in_relation.py data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt                 \
		| python scripts/spanish/select_sentences_with_non_adjacent_main_relation.py data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt \
		| java -cp lib/*:graph-parser.jar others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		| gzip > data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz

create_spanish_deplambda_format:
	zcat data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz \
		| python scripts/spanish/create-entity-mention-format.py \
		| gzip \
		> ../working/spanish_wikipedia.txt.gz

# Spanish WebQuestions Processing
entity_tag_spanish:
	python scripts/spanish/combine_wq_entities_spanish_english.py data/spanish_webquestions/webquestions.lexicon.txt data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz > data/spanish_webquestions/webquestions.lexicon.extended.txt
	cat data/spanish_webquestions/webquestions.examples.test.utterances_es \
		| java -cp lib/*:graph-parser.jar others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.test.utterances data/spanish_webquestions/webquestions.examples.test.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.test.domains.json \
		| java -cp lib/*:graph-parser.jar others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		> data/spanish_webquestions/webquestions.examples.test.es.json
	cat data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| java -cp lib/*:graph-parser.jar others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.train.utterances data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.train.domains.json \
		| java -cp lib/*:graph-parser.jar others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		> data/spanish_webquestions/webquestions.examples.train.es.json

create_spanish_splits:
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.train.es.json data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 > data/spanish_webquestions/webquestions.examples.business_film_people.train.json.txt
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.train.es.json data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 > data/spanish_webquestions/webquestions.examples.business_film_people.dev.json.txt
	python scripts/spanish/select_splits_from_english.py data/spanish_webquestions/webquestions.examples.test.es.json data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json > data/spanish_webquestions/webquestions.examples.business_film_people.test.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.train.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.train.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.dev.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.dev.json.txt
	cat data/spanish_webquestions/webquestions.examples.business_film_people.test.json.txt \
		| python scripts/spanish/create-entity-mention-format.py \
		> working/webquestions.es.examples.business_film_people.test.json.txt

## Unsupervised Parsing experiments
unsupervised_first_experiment:
	mkdir -p ../working/unsupervised_first_experiment
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
    -schema data/freebase/schema/business_film_people_schema.txt \
    -relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
    -lexicon data/dummy.txt \
    -ccgLexicon data/dummy.txt \
    -ccgIndexedMapping lib_data/ybisk-mapping.txt \
    -unaryRules data/dummy.txt \
    -binaryRules data/dummy.txt \
    -cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
    -domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
    -nthreads 20 \
    -trainingSampleSize 600 \
    -iterations 10 \
    -nBestTrainSyntacticParses 100 \
    -nBestTestSyntacticParses 1 \
    -nbestGraphs 100 \
    -debugEnabledFlag false \
    -useSchema true \
    -useKB true \
    -groundFreeVariables true \
    -useEmptyTypes false \
    -ignoreTypes true \
    -urelGrelFlag true \
    -urelPartGrelPartFlag false \
    -utypeGtypeFlag false \
    -gtypeGrelFlag false \
    -wordGrelPartFlag false \
    -wordBigramGrelPartFlag false \
    -argGrelPartFlag false \
    -stemMatchingFlag true \
    -mediatorStemGrelPartMatchingFlag false \
    -argumentStemMatchingFlag false \
    -argumentStemGrelPartMatchingFlag false \
    -graphIsConnectedFlag true \
    -graphHasEdgeFlag true \
    -countNodesFlag true \
    -edgeNodeCountFlag true \
    -duplicateEdgesFlag true \
    -grelGrelFlag true \
    -useLexiconWeightsRel false \
    -useLexiconWeightsType false \
    -validQueryFlag true \
    -initialEdgeWeight 1.0 \
    -initialTypeWeight -1.0 \
    -initialWordWeight 10.00 \
    -stemFeaturesWeight 0.0 \
    -useNbestGraphsFlag true \
    -endpoint localhost \
    -trainingCorpora "data/unsupervised/training/unsupervised_parser.json.noDeps.gz" \
    -logFile ../working/unsupervised_first_experiment/business_film_people.log.txt \
    > ../working/unsupervised_first_experiment/business_film_people.txt

candc_distant_eval:
	mkdir -p ../working/candc_distant_eval
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordBigramGrelPartFlag false \
	-argGrelPartFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag true \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 1.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/train.json.gz \
	-devFile data/distant_eval/dev.json.blank.gz \
	-logFile ../working/candc_distant_eval/business_film_people.log.txt \
	> ../working/candc_distant_eval/business_film_people.txt
