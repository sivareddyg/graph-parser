# Ignore
create_grounded_lexicon_and_filter_sentences_%:
	mkdir -p data/freebase/sentences_training
	mkdir -p data/freebase/grounded_lexicon
	zcat data/freebase/sentences_filtered/$*_sentences.txt.gz \
	| java -Xms2048m -cp .:GraphParser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/candc_markedup.modified \
	--unaryRulesFile data/unary_rules.txt \
	--binaryRulesFile data/binary_rules.txt \
	--specialCasesFile data/lexicon_specialCases.txt \
	--relationTypesFile data/freebase/stats/$*_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/$*_facts.txt.gz \
	--outputLexiconFile data/freebase/grounded_lexicon/$*_grounded_lexicon.txt \
	| gzip > data/freebase/sentences_training/$*_training_sentences.txt.gz

# Create training data and grounded lexicon
create_grounded_lexicon_and_filter_sentences:
	make create_grounded_lexicon_and_filter_sentences_business
	make create_grounded_lexicon_and_filter_sentences_film
	make create_grounded_lexicon_and_filter_sentences_people

# Create deplambda lexicon
create_deplambda_lexicon:
	mkdir -p data/freebase/sentences_training
	mkdir -p data/freebase/grounded_lexicon
	zcat data/deplambda/data-00085-of-00100.graphparser.gz \
	| java -Xms2048m -cp .:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey dependency_lambda \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/candc_markedup.modified \
	--unaryRulesFile data/unary_rules.txt \
	--binaryRulesFile data/binary_rules.txt \
	--specialCasesFile data/lexicon_specialCases.txt \
	--relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/freebase/grounded_lexicon/deplambda_grounded_lexicon.txt \
	| gzip > data/freebase/sentences_training/deplambda_training_sentences.txt.gz

# Create deplambda webquestions testing and training data
create_deplambda_supervised_input_data:
	cat data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json | python scripts/dump_sentences.py > data/deplambda/webquestions.train.txt
	cat data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json | python scripts/dump_sentences.py > data/deplambda/webquestions.test.txt
	cat data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json | python scripts/dependency_semantic_parser/create_entity_lexicon.py > data/deplambda/entity_lexicon.txt

copy_deplambda_output:
	rm data/deplambda/webquestions.test.documents.txt
	rm data/deplambda/webquestions.train.documents.txt
	rm -r data/deplambda/unsupervised
	fileutil cp /cns/lb-d/home/oscart/e/siva-tacl-data/graphparser-questions-test/output data/deplambda/webquestions.test.documents.txt
	fileutil cp /cns/lb-d/home/oscart/e/siva-tacl-data/graphparser-questions-train/output data/deplambda/webquestions.train.documents.txt
	fileutil cp /cns/lb-d/home/oscart/e/siva-tacl-data/graphparser-lambda-expressions/* data/deplambda/unsupervised

convert_deplambda_output_to_graphparser:
	cat data/deplambda/unsupervised/* | python scripts/dependency_semantic_parser/convert_document_json_graphparser_json.py | gzip > data/deplambda/unsupervised.graphparser.txt.gz
	cat data/deplambda/webquestions.train.documents.txt | python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_questions.py data/deplambda/entity_lexicon.txt | python scripts/dependency_semantic_parser/add_answers.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json > data/deplambda/webquestions.train.graphparser.txt
	cat data/deplambda/webquestions.test.documents.txt | python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_questions.py data/deplambda/entity_lexicon.txt | python scripts/dependency_semantic_parser/add_answers.py data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json > data/deplambda/webquestions.test.graphparser.txt
	cat data/deplambda/webquestions.train.documents.txt

# Ignore
filter_training_sentences_%:
	mkdir -p data/freebase/sentences_training_filtered
	zcat data/freebase/sentences_training/$*_training_sentences.txt.gz \
	|  python scripts/freebase-training/filter_training_sentences.py 0 0 0 0 0 \
	| gzip >  data/freebase/sentences_training_filtered/$*_training_sentences_filtered_00000.txt.gz

# filter training sentences - These are used for training
filter_training_sentences:
	make filter_training_sentences_business
	make filter_training_sentences_film
	make filter_training_sentences_people

# Merge lexicons
merge_lexicon:
	python scripts/freebase/merge_lexicon.py \
	data/freebase/grounded_lexicon/business_grounded_lexicon.txt \
	data/freebase/grounded_lexicon/film_grounded_lexicon.txt \
	data/freebase/grounded_lexicon/people_grounded_lexicon.txt \
	> data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt

# Web Questions Experiments

# Parse webquestions.
parse_webquestions_easyccg:
	cat data/webquestions/webquestions.examples.test.domains.filtered.json \
	| python scripts/webquestions-preprocessing/tokenise.py \
	| python scripts/webquestions-preprocessing/tag_named_entities_normal.py data/webquestions/webquestions.examples.test.domains.manual.lexicon \
	| java -cp .:tools/stanford-ner-2012-11-11/stanford-ner.jar:tools/stanford-ner-2012-11-11/gson-2.2.2.jar:tools/stanford-ner-2012-11-11/guava-14.0.1.jar:scripts NerJsonInputData \
	| python scripts/cleaning/ner_post_process.py \
	| python scripts/webquestions-preprocessing/non_copulize.py \
	| python scripts/webquestions-preprocessing/run_candc_tagger_ner_on_json_sentences_questions.py questions \
	| python scripts/webquestions-preprocessing/run_easyccg_parser_on_json_sentences_questions.py questions \
	> data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json
	
	cat data/webquestions/webquestions.examples.train.domains.filtered.json \
	| python scripts/webquestions-preprocessing/tokenise.py \
	| python scripts/webquestions-preprocessing/tag_named_entities_normal.py data/webquestions/webquestions.examples.train.domains.manual.lexicon \
	| java -cp .:tools/stanford-ner-2012-11-11/stanford-ner.jar:tools/stanford-ner-2012-11-11/gson-2.2.2.jar:tools/stanford-ner-2012-11-11/guava-14.0.1.jar:scripts NerJsonInputData \
	| python scripts/cleaning/ner_post_process.py \
	| python scripts/webquestions-preprocessing/non_copulize.py \
	| python scripts/webquestions-preprocessing/run_candc_tagger_ner_on_json_sentences_questions.py questions \
	| python scripts/webquestions-preprocessing/run_easyccg_parser_on_json_sentences_questions.py questions \
	> data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json
	
	python scripts/webquestions-preprocessing/training_split.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json
	head -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.200
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200
	
	head -n100 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.100
	tail -n100 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.100

# Paraphrased webquestions
parse_webquestions_easyccg_paraphrase:
	cat data/webquestions/webquestions.examples.test.domains.filtered.json \
	| python scripts/webquestions-preprocessing/tokenise.py \
	| python scripts/webquestions-preprocessing/tag_named_entities_normal.py data/webquestions/webquestions.examples.test.domains.manual.lexicon \
	| java -cp .:tools/stanford-ner-2012-11-11/stanford-ner.jar:tools/stanford-ner-2012-11-11/gson-2.2.2.jar:tools/stanford-ner-2012-11-11/guava-14.0.1.jar:scripts NerJsonInputData \
	| python scripts/cleaning/ner_post_process.py \
	| python scripts/webquestions-preprocessing/non_copulize.py \
	| python scripts/webquestions-preprocessing/paraphrase_rules.py \
	| python scripts/webquestions-preprocessing/run_candc_tagger_ner_on_json_sentences_questions.py questions \
	| python scripts/webquestions-preprocessing/run_easyccg_parser_on_json_sentences_questions.py questions \
	> data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.paraphrase.json
	
	cat data/webquestions/webquestions.examples.train.domains.filtered.json \
	| python scripts/webquestions-preprocessing/tokenise.py \
	| python scripts/webquestions-preprocessing/tag_named_entities_normal.py data/webquestions/webquestions.examples.train.domains.manual.lexicon \
	| java -cp .:tools/stanford-ner-2012-11-11/stanford-ner.jar:tools/stanford-ner-2012-11-11/gson-2.2.2.jar:tools/stanford-ner-2012-11-11/guava-14.0.1.jar:scripts NerJsonInputData \
	| python scripts/cleaning/ner_post_process.py \
	| python scripts/webquestions-preprocessing/non_copulize.py \
	| python scripts/webquestions-preprocessing/paraphrase_rules.py \
	| python scripts/webquestions-preprocessing/run_candc_tagger_ner_on_json_sentences_questions.py questions \
	| python scripts/webquestions-preprocessing/run_easyccg_parser_on_json_sentences_questions.py questions \
	> data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json
	
	python scripts/webquestions-preprocessing/training_split.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json
	head -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.80 > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.train.200  
	tail -n200 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.80 > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.dev.200
	
	head -n100 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.80 > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.train.100  
	tail -n100 data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.80 > data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.dev.100

# Main Experiments
#
# TACL MWG Baseline
tacl_mwg:
	mkdir -p working/tacl_mwg
	java -Xms2048m -cp .:GraphParser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 100 \
	-iterations 10 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
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
	-initialTypeWeight 1.0 \
	-initialWordWeight 1.0 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-devFile data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile working/tacl_mwg/business_film_people.log.txt \
	> working/tacl_mwg/business_film_people.txt

# TACL GraphPaser results
tacl_unsupervised:
	mkdir -p working/tacl_unsupervised
	java -Xms2048m -cp .:GraphParser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 600 \
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
	-initialTypeWeight 1.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora "data/freebase/sentences_training_filtered/business_training_sentences_filtered_00000.txt.gz;data/freebase/sentences_training_filtered/film_training_sentences_filtered_00000.txt.gz;data/freebase/sentences_training_filtered/people_training_sentences_filtered_00000.txt.gz" \
	-devFile data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile working/tacl_unsupervised/business_film_people.log.txt \
	> working/tacl_unsupervised/business_film_people.txt

# TACL GraphParser + Para results
tacl_unsupervised_paraphrase:
	mkdir -p working/tacl_unsupervised_paraphrase
	java -Xms2048m -cp .:GraphParser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/freebase/grounded_lexicon/business_film_people_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-trainingSampleSize 600 \
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
	-initialTypeWeight 1.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora "data/freebase/sentences_training_filtered/business_training_sentences_filtered_00000.txt.gz;data/freebase/sentences_training_filtered/film_training_sentences_filtered_00000.txt.gz;data/freebase/sentences_training_filtered/people_training_sentences_filtered_00000.txt.gz" \
	-devFile data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.paraphrase.json.dev.200 \
	-testFile data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.paraphrase.json \
	-logFile working/tacl_unsupervised_paraphrase/business_film_people.log.txt \
	> working/tacl_unsupervised_paraphrase/business_film_people.txt

# Deplambda results
deplambda_supervised:
	mkdir -p working/deplambda_supervised
	java -Xms2048m -Xmx20g -cp .:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile data/freebase/stats/business_film_people_relation_types.txt \
	-lexicon data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 1 \
	-trainingSampleSize 600 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 500 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables true \
	-useEmptyTypes true \
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
	-initialTypeWeight 1.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint oscart.hot.corp.google.com \
	-semanticParseKey dependency_lambda \
	-trainingCorpora "data/dummy.txt.gz" \
	-supervisedCorpus data/deplambda/webquestions.train.graphparser.txt \
	-testFile data/deplambda/webquestions.test.graphparser.txt \
	-logFile working/deplambda_supervised/business_film_people.log.txt \
	> working/deplambda_supervised/business_film_people.txt


