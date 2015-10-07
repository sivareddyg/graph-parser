# Extract clueweb sentences
extract_sentences_with_multiple_entities:
	python scripts/freebase/extract_clueweb_sentences_containing_entities.py \
        ../data/clueweb/CLUEWEB09_1/wiki00/ \
        ../data/clueweb/freebase_annotated_data/ClueWeb09_English_1 \
        | gzip > ../data/clueweb/wiki-sentences.json.txt.00.gz

process_clueweb_split_%:
	#python scripts/freebase/extract_clueweb_sentences_containing_entities.py \
        #working/clueweb/ClueWeb09_English_1/ \
        #../data/clueweb/freebase_annotated_data/ClueWeb09_English_1_split$* \
        #| gzip > ../data/clueweb/ClueWeb09_1-sentences.json.txt.$*.gz
	# python scripts/freebase/merge_duplicate_sentences.py ../data/clueweb/ClueWeb09_1-sentences.json.txt.$*.gz | gzip > ../data/clueweb/ClueWeb09_1-sentences.cleaned.json.txt.$*.gz
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.json.txt.$*.gz \
		| java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunTokenizerOnEntityTaggedClueweb \
		| python scripts/cleaning/remove_longer_sentences.py \
		| python scripts/cleaning/remove_duplicate_sentences.py \
		| java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunPosTaggerAndNerWithoutTokenizerPipeline \
		| python scripts/run_batch_process.py "java -cp lib/*:bin in.sivareddy.scripts.RunEasyCCG 1" 100000 \
		| gzip > ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz

extract_easyccg_lexicon_clueweb_split_%:
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz \
	| python scripts/run_batch_process.py \
		"java -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
		-endpoint localhost \
		-schema data/freebase/schema/all_domains_schema.txt \
		-typeKey "fb:type.object.type" \
		-nthreads 40 \
		-ignoreTypes true" \
		100000 \
	| gzip > ../data0/clueweb/ClueWeb09_1-lexicon.txt.$*.gz

extract_wiki_lexicon:
	zcat ../data/clueweb/wiki-sentences.cleaned.json.txt.00.gz  ../data/clueweb/wiki-sentences.cleaned.json.txt.01.gz  ../data/clueweb/wiki-sentences.cleaned.json.txt.02.gz ../data/clueweb/wiki-sentences.cleaned.json.txt.03.gz \
        | java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunTokenizerOnEntityTaggedClueweb \
        | python scripts/cleaning/remove_longer_sentences.py \
        | python scripts/cleaning/remove_duplicate_sentences.py \
        | java -cp lib/*:bin in.sivareddy.scripts.clueweb.RunPosTaggerAndNerWithoutTokenizerPipeline \
        | python scripts/run_batch_process.py "java -cp lib/*:bin in.sivareddy.scripts.RunEasyCCG 1" 100000 \
        | gzip > ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.wiki.gz
	make extract_easyccg_lexicon_clueweb_split_wiki

merge_easyccg_lexicon:
	zcat ../data0/clueweb/ClueWeb09_1-lexicon.txt.1.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.2.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.3.gz ../data0/clueweb/ClueWeb09_1-lexicon.txt.wiki.gz \
		|	python scripts/freebase/merge_lexicon.py \
	    | gzip > data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt.gz

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

entity_span_tag_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/webquestions-preprocessing/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/webquestions/webquestions.examples.train.domains.entity.matches.json
	cat data/webquestions/webquestions.examples.test.domains.json \
		| python scripts/entity-annotation/convert_utterance_to_sentence.py \
		| python scripts/webquestions-preprocessing/add_gold_mid_using_gold_url.py data/freebase/mid_to_key.txt.gz \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
	   	| java -cp lib/*:bin others.StanfordEnglishPipelineCaseless \
		| java -cp lib/*:bin in.sivareddy.scripts.NounPhraseAnnotator EN_PTB \
		> data/webquestions/webquestions.examples.test.domains.entity.matches.json

add_gold_relations:
		cat data/webquestions/webquestions.examples.train.domains.entity.matches.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> working/webquestions.examples.train.domains.entity.matches.json
		mv working/webquestions.examples.train.domains.entity.matches.json data/webquestions/webquestions.examples.train.domains.entity.matches.json

		cat data/webquestions/webquestions.examples.test.domains.entity.matches.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.AddGoldRelationsToWebQuestionsData localhost data/freebase/schema/all_domains_schema.txt \
		> working/webquestions.examples.test.domains.entity.matches.json
		mv working/webquestions.examples.test.domains.entity.matches.json data/webquestions/webquestions.examples.test.domains.entity.matches.json

rank_entity_webq_data:
	cat data/webquestions/webquestions.examples.train.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.json \
	| java -cp lib/*:bin in.sivareddy.graphparser.util.RankMatchedEntities \
	> data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json

disambiguate_entities_webq_data:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.json

disambiguate_entities_webq_data_second_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.2.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.2.json

disambiguate_entities_webq_data_third_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.2.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity true \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.2.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

replace_unknown_mids:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/train.txt
	mv working/train.txt data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json

	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin/ in.sivareddy.scripts.MapNewMidToOldMid data/freebase/entities_sempre.txt.gz data/freebase/mid_to_key.txt.gz data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/test.txt
	mv working/test.txt data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

entity_disambiguation_results:
	python scripts/entity-annotation/evaluate_entity_annotation.py < data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json | less

copy_gold_relations:
	python scripts/entity-annotation/copy_gold_relations.py data/webquestions/webquestions.examples.train.domains.entity.matches.json \
		< data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		> working/webquestions.examples.train.domains.entity.disambiguated.3.json
	mv working/webquestions.examples.train.domains.entity.disambiguated.3.json data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json

	python scripts/entity-annotation/copy_gold_relations.py data/webquestions/webquestions.examples.test.domains.entity.matches.json \
		< data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		> working/webquestions.examples.test.domains.entity.disambiguated.3.json
	mv working/webquestions.examples.test.domains.entity.disambiguated.3.json data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json

disambiguate_entities_webq_data_fourth_pass:
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		-outputFile data/webquestions/webquestions.examples.train.domains.entity.disambiguated.4.json
	java -cp lib/*:bin in.sivareddy.graphparser.cli.RunDisambiguateEntities \
		-endpoint localhost \
		-typeKey "fb:type.object.type" \
		-schema data/freebase/schema/all_domains_schema.txt \
		-initialNbest 10 \
		-intermediateNbest 10 \
		-finalNbest 10 \
		-entityHasReadableId true \
		-nthreads 10 \
		-noSucceedingNamedEntity false \
		-noPrecedingNamedEntity false \
		-containsNamedEntity false \
		-shouldStartWithNamedEntity false \
		-inputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		-outputFile data/webquestions/webquestions.examples.test.domains.entity.disambiguated.4.json

entity_dismabiguated_webq_to_graphparser_forrest:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		> working/webquestions.train.full.pass3.txt

	cat working/webquestions.train.full.pass3.txt \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt

	cat working/webquestions.train.full.pass3.txt \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt

	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		> working/webquestions.test.full.pass3.txt

	cat working/webquestions.test.full.pass3.txt \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt

split_forest_to_sentences:
	cat data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
		> working/webquestions.automaticDismabiguation.train.pass3.json.txt
	cat data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.graphparser.util.SplitForrestToSentences \
		|java -cp lib/*:bin in.sivareddy.graphparser.util.MergeEntity \
		> working/webquestions.automaticDismabiguation.test.pass3.json.txt

full_data_to_xukun:
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.20 > working/dev.txt
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.80 > working/training.txt
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt > working/test.txt

webq_to_oscar:
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.20 > working/dev.txt
	cat working/dev.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.dev.split.json.txt
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.80 > working/training.txt
	cat working/training.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.train.split.json.txt
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt | python scripts/extract_subset.py  ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt > working/test.txt
	cat working/test.txt | python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py > working/webquestions.vanilla.freebaseAPIannotations.test.split.json.txt

clueweb_to_deplamba_%:
	zcat ../data0/clueweb/ClueWeb09_1-sentences.cleaned.parsed.json.txt.$*.gz \
		| python scripts/cleaning/filter_sentences_with_no_parses.py \
		| python scripts/convert-graph-parser-to-entity-mention-format_with_answers.py \
		| gzip > working/ClueWeb09_1-sentences.$*.gz

convert_deplambda_to_gp_forest:
	cat working/webquestions.vanilla.freebaseAPIannotations.dev.split.singletype_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt 
	cat working/webquestions.vanilla.freebaseAPIannotations.training.split.singletype_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt 
	cat working/webquestions.vanilla.freebaseAPIannotations.test.split.singletype_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt 

	cat working/webquestions.vanilla.freebaseAPIannotations.dev.split.old_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.old.json.txt 
	cat working/webquestions.vanilla.freebaseAPIannotations.train.split.old_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.old.json.txt 
	cat working/webquestions.vanilla.freebaseAPIannotations.test.split.old_lambdas.json.txt \
		| python scripts/dependency_semantic_parser/convert_document_json_graphparser_json_new.py \
		| java -cp lib/*:bin in.sivareddy.scripts.MergeSplitsToForest working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		> data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.old.json.txt 

data_to_xukun:
	cat working/webquestions.automaticDismabiguation.test.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.tacl.json \
		> working/webquestions.automaticDismabiguation.test.pass3.taclSubset.json.txt 
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915 \
		> working/webquestions.automaticDismabiguation.train.pass3.taclSubset.json.txt 
	cat working/webquestions.automaticDismabiguation.train.pass3.json.txt \
		| python scripts/extract_subset.py data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.200 \
		> working/webquestions.automaticDismabiguation.dev.pass3.taclSubset.json.txt 

evaluate_bow_goldMid_goldRel:
	mkdir -p data/outputs/
	# cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	#	| java -cp lib/*:bin in.sivareddy.scripts.EvaluateBoWOracleUsingGoldMidAndGoldRelations localhost data/freebase/schema/all_domains_schema.txt \
	#	> data/outputs/bow_goldMid_goldRel.trainAndDev.answers.txt

	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
		| java -cp lib/*:graph-parser.jar in.sivareddy.scripts.EvaluateBoWOracleUsingGoldMidAndGoldRelations localhost data/freebase/schema/all_domains_schema.txt \
		> data/outputs/bow_goldMid_goldRel.train.answers.txt

	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		| java -cp lib/*:graph-parser.jar in.sivareddy.scripts.EvaluateBoWOracleUsingGoldMidAndGoldRelations localhost data/freebase/schema/all_domains_schema.txt \
		> data/outputs/bow_goldMid_goldRel.dev.answers.txt

	cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
		| java -cp lib/*:graph-parser.jar in.sivareddy.scripts.EvaluateBoWOracleUsingGoldMidAndGoldRelations localhost data/freebase/schema/all_domains_schema.txt \
		> data/outputs/bow_goldMid_goldRel.test.answers.txt

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
		| java -cp lib/*:bin others.RunEasyCCG \
		> data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json
	head -n915 data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json \
		> data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.915
	tail -n200 data/webquestions/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json \
		> data/tacl/vanilla_one_best/webquestions.examples.train.domains.entity.matches.ranked.1best.merged.tacl.json.200
	cat data/webquestions/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.json \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json \
		| java -cp lib/*:bin others.RunEasyCCG \
		> data/tacl/vanilla_one_best/webquestions.examples.test.domains.entity.matches.ranked.1best.merged.tacl.json

extact_tacl_subset_vanilla_gold:
	mkdir -p data/tacl_splits/webquestions/
	cat data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		| python scripts/extract_subset.py data/webquestions/webquestions.examples.train.domains.easyccg.parse.filtered.json \
		> working/vanilla_full.txt
	head -n915 working/vanilla_full.txt > data/tacl_splits/webquestions/vanilla_train.txt
	tail -n200 working/vanilla_full.txt > data/tacl_splits/webquestions/vanilla_dev.txt
	cat data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt \
        | python scripts/extract_subset.py data/webquestions/webquestions.examples.test.domains.easyccg.parse.filtered.json \
        > data/tacl_splits/webquestions/vanilla_test.txt

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
	cat ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.80 | java -cp lib/*:bin others.RunEasyCCG > data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt
	cat ../FreePar/data/webquestions/webquestions.train.all.entity_annotated.vanilla.txt.20 | java -cp lib/*:bin others.RunEasyCCG > data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt
	cat ../FreePar/data/webquestions/webquestions.test.all.entity_annotated.vanilla.txt | java -cp lib/*:bin others.RunEasyCCG > data/complete/vanilla_gold/webquestions.vanilla.test.full.easyccg.json.txt
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
	head -n915 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCG > data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915
	tail -n200 data/webquestions/webquestions.examples.train.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCG > data/tacl/vanilla_gold/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200
	cat data/webquestions/webquestions.examples.test.domains.filtered.vanilla.json | java -cp lib/*:bin others.RunEasyCCG > data/tacl/vanilla_gold/webquestions.examples.test.domains.easyccg.parse.filtered.json

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
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
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
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
deplambda_supervised_vanilla_gold:
	rm -rf ../working/deplambda_supervised_vanilla_gold
	mkdir -p ../working/deplambda_supervised_vanilla_gold
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -agentlib:hprof=cpu=samples,interval=20,depth=3 -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.5 \
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
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus  data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-devFile data/complete/vanilla_gold/webquestions.vanilla.dev.full.deplambda.json.txt \
	-testFile data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-logFile ../working/deplambda_supervised_vanilla_gold_full/all.log.txt \
	> ../working/deplambda_supervised_vanilla_gold_full/all.txt

deplambda_supervised_vanilla_gold_bag_of_words:
	rm -rf ../working/deplambda_supervised_vanilla_gold_bag_of_words
	mkdir -p ../working/deplambda_supervised_vanilla_gold_bag_of_words
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.5 \
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
	-addBagOfWordsGraph true \
	-addOnlyBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-groundFreeVariables true \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus  data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-devFile data/complete/vanilla_gold/webquestions.vanilla.dev.full.deplambda.json.txt \
	-testFile data/complete/vanilla_gold/webquestions.vanilla.train.full.deplambda.json.txt \
	-logFile ../working/deplambda_supervised_vanilla_gold_bag_of_words/all.log.txt \
	> ../working/deplambda_supervised_vanilla_gold_bag_of_words/all.txt

# Deplambda results with unsupervised lexicon.
deplambda_supervised_with_unsupervised_lexicon:
	mkdir -p ../working/deplambda_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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


### WebQuestions complete data experiments ###
easyccg_supervised_without_merge:
	rm -rf ../working/easyccg_supervised_without_merge
	mkdir -p ../working/easyccg_supervised_without_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/ccg_without_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_supervised_without_merge/all.log.txt \
	> ../working/easyccg_supervised_without_merge/all.txt

easyccg_supervised_with_merge:
	rm -rf ../working/easyccg_supervised_with_merge
	mkdir -p ../working/easyccg_supervised_with_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/ccg_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_supervised_with_merge/all.log.txt \
	> ../working/easyccg_supervised_with_merge/all.txt

easyccg_mwg:
	rm -rf ../working/easyccg_mwg
	mkdir -p ../working/easyccg_mwg
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/complete/grounded_lexicon/easyccg_grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging false \
	-evaluateBeforeTraining true \
	-evaluateOnlyTheFirstBest false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/easyccg_mwg/all.log.txt \
	> ../working/easyccg_mwg/all.txt

### Paraphrasing experiments ##
create_paraphrases_gold_graphs:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_paraphrase_without_merge.dev \
		false \
		< data/paraphrasing/webq.paraphrases.dev.txt \
		> data/outputs/ccg_paraphrase_without_merge.dev.answers.txt

	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_paraphrase_without_merge.train \
		false \
		< data/paraphrasing/webq.paraphrases.train.txt \
		> data/outputs/ccg_paraphrase_without_merge.train.answers.txt

create_mt_paraphrases_gold_graphs:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_mt_paraphrase_without_merge.dev \
		false \
		< data/paraphrasing/webq.mt.paraphrases.dev.txt \
		> data/outputs/ccg_mt_paraphrase_without_merge.dev.answers.txt

	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_mt_paraphrase_without_merge.train \
		false \
		< data/paraphrasing/webq.mt.paraphrases.train.txt \
		> data/outputs/ccg_mt_paraphrase_without_merge.train.answers.txt

easyccg_supervised_paraphrase_without_merge:
	rm -rf ../working/easyccg_supervised_paraphrase_without_merge
	mkdir -p ../working/easyccg_supervised_paraphrase_without_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
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
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-paraphraseScoreFlag true \
	-forestSize 100 \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/paraphrasing/webq.paraphrases.train.txt \
	-goldParsesFile data/gold_graphs/ccg_paraphrase_without_merge.train.ser \
	-devFile data/paraphrasing/webq.paraphrases.dev.txt \
	-testFile data/paraphrasing/webq.paraphrases.test.txt \
	-logFile ../working/easyccg_supervised_paraphrase_without_merge/all.log.txt \
	> ../working/easyccg_supervised_paraphrase_without_merge/all.txt

easyccg_supervised_mt_paraphrase_without_merge:
	rm -rf ../working/easyccg_supervised_mt_paraphrase_without_merge
	mkdir -p ../working/easyccg_supervised_mt_paraphrase_without_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey synPars \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
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
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-evaluateBeforeTraining false \
	-evaluateOnlyTheFirstBest true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-paraphraseScoreFlag true \
	-forestSize 100 \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/paraphrasing/webq.mt.paraphrases.train.txt \
	-goldParsesFile data/gold_graphs/ccg_mt_paraphrase_without_merge.train.ser \
	-devFile data/paraphrasing/webq.mt.paraphrases.dev.txt \
	-testFile data/paraphrasing/webq.mt.paraphrases.test.txt \
	-logFile ../working/easyccg_supervised_mt_paraphrase_without_merge/all.log.txt \
	> ../working/easyccg_supervised_mt_paraphrase_without_merge/all.txt

deplambda_singletype_supervised_without_merge:
	rm -rf ../working/deplambda_singletype_supervised_without_merge
	mkdir -p ../working/deplambda_singletype_supervised_without_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-allowMerging false \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_without_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_without_merge/all.log.txt \
	> ../working/deplambda_singletype_supervised_without_merge/all.txt

deplambda_singletype_supervised_with_merge:
	rm -rf ../working/deplambda_singletype_supervised_with_merge
	mkdir -p ../working/deplambda_singletype_supervised_with_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/deplambda/clueweb-subset-academic-grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining true \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge/all.txt

deplambda_singletype_supervised_with_merge_wordGrelPart:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_wordGrelPart
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_wordGrelPart
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 4 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_wordGrelPart/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_wordGrelPart/all.txt


deplambda_singletype_supervised_with_merge_eventType:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_eventType
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_eventType
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 4 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_eventType/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_eventType/all.txt

deplambda_singletype_supervised_with_merge_argGrel:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_argGrel
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_argGrel
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 4 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag true \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_argGrel/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_argGrel/all.txt

deplambda_singletype_supervised_with_merge_urelGrel:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_urelGrel.1
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_urelGrel.1
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 4 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_urelGrel.1/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_urelGrel.1/all.txt

deplambda_singletype_supervised_with_merge_urelGrelPart:
	rm -rf ../working/deplambda_singletype_supervised_with_merge_urelGrelPart
	mkdir -p ../working/deplambda_singletype_supervised_with_merge_urelGrelPart
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_lambda \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-schema data/freebase/schema/all_domains_schema.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 2000 \
	-iterations 4 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag false \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-allowMerging true \
	-handleEventEventEdges true \
	-useBackOffGraph true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.05 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
	-goldParsesFile data/gold_graphs/deplambda_singletype_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.singletype.json.txt \
	-logFile ../working/deplambda_singletype_supervised_with_merge_urelGrelPart/all.log.txt \
	> ../working/deplambda_singletype_supervised_with_merge_urelGrelPart/all.txt



bow_supervised:
	rm -rf ../working/bow_supervised
	mkdir -p ../working/bow_supervised
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph true \
	-ngramGrelPartFlag true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag false \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-useLexiconWeightsRel false \
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
	-goldParsesFile data/gold_graphs/bow_without_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.json.txt \
	-logFile ../working/bow_supervised/all.log.txt \
	> ../working/bow_supervised/all.txt

dependency_with_merge:
	rm -rf ../working/dependency_with_merge
	mkdir -p ../working/dependency_with_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.old.json.txt \
	-goldParsesFile data/gold_graphs/dependency_with_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.old.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.old.json.txt \
	-logFile ../working/dependency_with_merge/all.log.txt \
	> ../working/dependency_with_merge/all.txt

dependency_without_merge:
	rm -rf ../working/dependency_without_merge
	mkdir -p ../working/dependency_without_merge
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-pointWiseF1Threshold 0.2 \
	-semanticParseKey dependency_question_graph \
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
	-forestSize 10 \
	-ngramLength 2 \
	-useSchema true \
	-useKB true \
	-addBagOfWordsGraph false \
	-ngramGrelPartFlag true \
	-groundFreeVariables false \
	-groundEntityVariableEdges false \
	-groundEntityEntityEdges false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag true \
	-argGrelPartFlag true \
	-argGrelFlag false \
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
	-useGoldRelations true \
	-evaluateOnlyTheFirstBest true \
	-evaluateBeforeTraining false \
	-entityScoreFlag true \
	-entityWordOverlapFlag true \
	-allowMerging false \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.old.json.txt \
	-goldParsesFile data/gold_graphs/dependency_without_merge.train.ser \
	-devFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.old.json.txt \
	-testFile data/complete/vanilla_automatic/webquestions.automaticDismabiguation.test.pass3.deplambda.old.json.txt \
	-logFile ../working/dependency_without_merge/all.log.txt \
	> ../working/dependency_without_merge/all.txt

############################################### TACL Experiments  ################################################
# TACL MWG Baseline
tacl_mwg:
	mkdir -p ../working/tacl_mwg
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag true \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-relationTypesFile data/dummy.txt \
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
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-eventTypeGrelPartFlag false \
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
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-trainingCorpora data/tacl/sentences_training/tacl_training_sentences.txt.gz \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_unsupervised/business_film_people.log.txt \
	> ../working/tacl_unsupervised/business_film_people.txt

tacl_unsupervised_loaded_model:
	mkdir -p ../working/tacl_unsupervised_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag false \
	-eventTypeGrelPartFlag false \
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
	-useLexiconWeightsType false \
	-validQueryFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-loadModelFromFile ../working/tacl_unsupervised/business_film_people.log.txt.model.bestIteration \
	-groundInputCorpora data/tacl/sentences_training/tacl_training_sentences.txt.gz \
	-logFile ../working/tacl_unsupervised_loaded_model/business_film_people.log.txt \
	> ../working/tacl_unsupervised_loaded_model/business_film_people.txt

tacl_supervised:
	rm -rf ../working/tacl_supervised
	mkdir -p ../working/tacl_supervised
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/dummy.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 10 \
	-trainingSampleSize 500 \
	-iterations 10 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-ngramLength 2 \
	-ngramGrelPartFlag true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-eventTypeGrelPartFlag true \
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
	-validQueryFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised/business_film_people.log.txt \
	> ../working/tacl_supervised/business_film_people.txt

tacl_supervised_with_unsupervised_lexicon:
	rm -rf ../working/tacl_supervised_with_unsupervised_lexicon
	mkdir -p ../working/tacl_supervised_with_unsupervised_lexicon
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-ccgLexiconQuestions lib_data/lexicon_specialCases_questions_vanilla.txt \
	-relationTypesFile data/dummy.txt \
	-lexicon data/tacl/grounded_lexicon/tacl_grounded_lexicon.txt \
	-domain "http://rdf.freebase.com" \
	-typeKey "fb:type.object.type" \
	-nthreads 20 \
	-trainingSampleSize 500 \
	-iterations 10 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes true \
	-ngramLength 2 \
	-ngramGrelPartFlag true \
	-urelGrelFlag true \
	-urelPartGrelPartFlag true \
	-utypeGtypeFlag false \
	-gtypeGrelFlag false \
	-wordGrelPartFlag true \
	-eventTypeGrelPartFlag true \
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
	-validQueryFlag true \
	-initialEdgeWeight -0.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.05 \
	-endpoint localhost \
	-supervisedCorpus data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.train.915 \
	-devFile data/tacl/webquestions.examples.train.domains.easyccg.parse.filtered.json.dev.200 \
	-testFile data/tacl/webquestions.examples.test.domains.easyccg.parse.filtered.json \
	-logFile ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.log.txt \
	> ../working/tacl_supervised_with_unsupervised_lexicon/business_film_people.txt

tacl_unsupervised_free917:
	mkdir -p ../working/tacl_unsupervised_free917
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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

tacl_supervised_vanilla_gold:
	rm -rf ../working/tacl_supervised_vanilla_gold
	mkdir -p ../working/tacl_supervised_vanilla_gold
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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


tacl_supervised_with_unsupervised_lexicon_vanilla_gold_online:
	rm -rf ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold
	mkdir -p ../working/tacl_supervised_with_unsupervised_lexicon_vanilla_gold
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
tacl_unsupervised_loaded_model_old:
	mkdir -p ../working/tacl_unsupervised_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
	-eventTypeGrelPartFlag false \
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
	zcat data/freebase/spanish/spanish_business_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_business_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_film_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_film_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_people_entities.txt.gz | java -cp lib/*:bin others.SpanishEntityTokenizer | gzip > data/freebase/spanish/spanish_people_entities.tokenized.txt.gz
	zcat data/freebase/spanish/spanish_business_entities.tokenized.txt.gz data/freebase/spanish/spanish_film_entities.tokenized.txt.gz data/freebase/spanish/spanish_people_entities.tokenized.txt.gz | gzip > data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz

extract_spanish_sentences:
	bzcat data/bravas/extracted/AA/wiki_00.bz2 \
		| java -cp lib/*:bin others.SpanishTokenizer                 \
		| perl -pe 's|=LRB=.*?=RRB=||g'                 \
		| grep -v =LRB= | grep -v =RRB=                 \
		| python scripts/spanish/select_sentences_with_entities_in_relation.py data/freebase/spanish/spanish_business_film_people_entities.tokenized.txt.gz data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt                 \
		| python scripts/spanish/select_sentences_with_non_adjacent_main_relation.py data/freebase/domain_facts/business_film_people_facts.txt.gz data/freebase/schema/business_film_people_schema.txt \
		| java -cp lib/*:bin others.SpanishPosAndNer \
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
		| java -cp lib/*:bin others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.test.utterances data/spanish_webquestions/webquestions.examples.test.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.test.domains.json \
		| java -cp lib/*:bin others.SpanishPosAndNer \
		| python scripts/spanish/process_named_entities.py \
		> data/spanish_webquestions/webquestions.examples.test.es.json
	cat data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| java -cp lib/*:bin others.SpanishTokenizerEol \
		| python scripts/spanish/annotate_entities_maximal_string_matching.py data/spanish_webquestions/webquestions.lexicon.extended.txt \
		| python scripts/spanish/add_sentences.py data/spanish_webquestions/webquestions.examples.train.utterances data/spanish_webquestions/webquestions.examples.train.utterances_es \
		| python scripts/spanish/merge_english_annotations.py data/webquestions/webquestions.examples.train.domains.json \
		| java -cp lib/*:bin others.SpanishPosAndNer \
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

clean_data:
	zcat data/distant_eval/test.json.blank.gz | python scripts/cleaning/unannonate_named_entities.py > data/distant_eval/test.json.blank
	gzip data/distant_eval/test.json.blank
	zcat data/distant_eval/test.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/test.json.blank
	gzip data/distant_eval/test.json.blank
	zcat data/distant_eval/train.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/train.json.blank
	gzip data/distant_eval/train.json.blank
	zcat data/distant_eval/dev.json.blank.gz | python scripts/cleaning/remove_sentences_with_consecutive_entities.py > data/distant_eval/dev.json.blank
	gzip data/distant_eval/dev.json.blank

clean_unsup_data:
	zcat data/distant_eval/unsupervised_syntax/dev.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/dev.json.blank
	gzip data/distant_eval/unsupervised_syntax/dev.json.blank

	zcat data/distant_eval/unsupervised_syntax/train.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/train.json.blank
	gzip data/distant_eval/unsupervised_syntax/train.json.blank


clean_test_data:
	zcat data/distant_eval/semi_supervised_syntax/test.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/semi_supervised_syntax/test.json.blank
	gzip data/distant_eval/semi_supervised_syntax/test.json.blank

	zcat data/distant_eval/unsupervised_syntax/test.json.blank.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		> data/distant_eval/unsupervised_syntax/test.json.blank
	gzip data/distant_eval/unsupervised_syntax/test.json.blank

create_blank_spanish:
	zcat data/distant_eval/spanish/semisup/train.json.gz \
		| python scripts/createBLANK.py \
		| gzip > data/distant_eval/spanish/semisup/train.json.blank.gz

unsupervised_round_2_data:
	zcat data/distant_eval/unsupervised_syntax_round_2/train.json.gz \
		| python scripts/createBLANK.py \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/train.json.blank.gz
	zcat data/distant_eval/unsupervised_syntax_round_2/test.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/test.json.blank.gz
	zcat data/distant_eval/unsupervised_syntax_round_2/dev.json.gz \
		| sed -e 's/ _blank_ NN / _blank_ NNP /g' \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities.py \
		| python scripts/cleaning/unannonate_named_entities.py \
		| gzip > data/distant_eval/unsupervised_syntax_round_2/dev.json.blank.gz

clean_spanish_data:
	zcat data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.txt.gz \
		| python scripts/spanish/merge_entity_words_to_one_entity.py \
		| python scripts/spanish/filter_sentences_with_less_than_two_entities.py \
		| python scripts/cleaning/remove_sentences_with_consecutive_entities_spanish.py \
		| gzip > data/freebase/spanish/spanish_wikipedia_business_film_people_sentences.json.filtered.txt.gz

unsupervised_first_experiment:
	mkdir -p ../working/unsupervised_first_experiment
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
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
    -eventTypeGrelPartFlag false \
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

create_candc_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/candc_markedup.modified \
	--unaryRulesFile lib_data/unary_rules.txt \
	--binaryRulesFile lib_data/binary_rules.txt \
	--specialCasesFile lib_data/lexicon_specialCases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	> /dev/null

create_semisup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/semi_supervised_syntax/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/ybisk-semi-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	> /dev/null

create_spanish_semisup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/spanish/semisup/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers word \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile lib_data/ybisk-semi-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile data/distant_eval/spanish/semisup/lexicon_fullSpecialCases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/spanish_semisup_grounded_lexicon.txt \
	> /dev/null

create_unsup_grounded_lexicon:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/unsupervised_syntax/train.json.gz \
	| java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	> /dev/null

create_unsup_grounded_lexicon_round_2:
	mkdir -p data/distant_eval/grounded_lexicon
	zcat data/distant_eval/unsupervised_syntax_round_2/train.json.gz \
	| java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunPrintDomainLexicon \
	--relationLexicalIdentifiers lemma \
	--semanticParseKey synPars \
	--argumentLexicalIdentifiers mid \
	--candcIndexFile data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	--unaryRulesFile lib_data/dummy.txt \
	--binaryRulesFile lib_data/dummy.txt \
	--specialCasesFile lib_data/ybisk-specialcases.txt \
	--relationTypesFile data/dummy.txt \
	--kbZipFile data/freebase/domain_facts/business_film_people_facts.txt.gz \
	--outputLexiconFile data/distant_eval/grounded_lexicon/unsup_grounded_lexicon_round_2.txt \
	> /dev/null

candc_distant_eval:
	rm -rf ../working/candc_distant_eval
	mkdir -p ../working/candc_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 50 \
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
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/train.json.blank.gz \
	-devFile data/distant_eval/dev.json.1000.blank.gz \
	-logFile ../working/candc_distant_eval/business_film_people.log.txt \
	> ../working/candc_distant_eval/business_film_people.txt

candc_distant_eval_loaded_model:
	rm -rf ../working/candc_distant_eval_loaded_model
	mkdir -p ../working/candc_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/distant_eval/grounded_lexicon/candc_grounded_lexicon.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
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
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.0 \
	-endpoint localhost \
	-loadModelFromFile ../working/candc_distant_eval.9/business_film_people.log.txt.model.iteration45 \
	-devFile data/distant_eval/dev.json.blank.gz \
	-testFile data/distant_eval/test.json.blank.gz \
	-logFile ../working/candc_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/candc_distant_eval_loaded_model/business_film_people.txt

bow_distant_eval:
	rm -rf ../working/bow_distant_eval
	mkdir -p ../working/bow_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/train.json.blank.gz \
	-devFile data/distant_eval/dev.json.1000.blank.gz \
	-logFile ../working/bow_distant_eval/business_film_people.log.txt \
	> ../working/bow_distant_eval/business_film_people.txt

bow_distant_eval_loaded_model:
	rm -rf ../working/bow_distant_eval_loaded_model
	mkdir -p ../working/bow_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/bow_distant_eval.4/business_film_people.log.txt.model.iteration6 \
	-devFile data/distant_eval/dev.json.blank.gz \
	-testFile data/distant_eval/test.json.blank.gz \
	-logFile ../working/bow_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/bow_distant_eval_loaded_model/business_film_people.txt

semisup_specialcases_distant_eval:
	rm -rf ../working/semisup_specialcases_distant_eval
	mkdir -p ../working/semisup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/semi_supervised_syntax/train.json.blank.gz \
	-devFile data/distant_eval/semi_supervised_syntax/dev.json.1000.blank.gz \
	-logFile ../working/semisup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/semisup_specialcases_distant_eval/business_film_people.txt

semisup_specialcases_distant_eval_loaded_model:
	rm -rf ../working/semisup_specialcases_distant_eval_loaded_model
	mkdir -p ../working/semisup_specialcases_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/semisup_specialcases_distant_eval/business_film_people.log.txt.model.iteration82 \
	-devFile data/distant_eval/semi_supervised_syntax/dev.json.blank.gz \
	-testFile data/distant_eval/semi_supervised_syntax/test.json.blank.gz \
	-logFile ../working/semisup_specialcases_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/semisup_specialcases_distant_eval_loaded_model/business_film_people.txt

unsup_specialcases_distant_eval:
	rm -rf ../working/unsup_specialcases_distant_eval
	mkdir -p ../working/unsup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/unsupervised_syntax/train.json.blank.gz \
	-devFile data/distant_eval/unsupervised_syntax/dev.json.1000.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval/business_film_people.txt

unsup_specialcases_distant_eval_round_2:
	rm -rf ../working/unsup_specialcases_distant_eval_round_2
	mkdir -p ../working/unsup_specialcases_distant_eval_round_2
	java -Xms2048m -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon_round_2.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/unsupervised_syntax_round_2/train.json.blank.gz \
	-devFile data/distant_eval/unsupervised_syntax_round_2/dev.json.1000.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval_round_2/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval_round_2/business_film_people.txt

unsup_specialcases_distant_eval_loaded_model:
	rm -rf ../working/unsup_specialcases_distant_eval_loaded_model
	mkdir -p ../working/unsup_specialcases_distant_eval_loaded_model
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping data/distant_eval/unsupervised_syntax/ybisk-mapping.txt \
	-ccgLexicon lib_data/ybisk-specialcases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/unsup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 0 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-loadModelFromFile ../working/unsup_specialcases_distant_eval/business_film_people.log.txt.model.iteration74 \
	-devFile data/distant_eval/unsupervised_syntax/dev.json.blank.gz \
	-testFile data/distant_eval/unsupervised_syntax/test.json.blank.gz \
	-logFile ../working/unsup_specialcases_distant_eval_loaded_model/business_film_people.log.txt \
	> ../working/unsup_specialcases_distant_eval_loaded_model/business_film_people.txt

distant_evaluation:
	python scripts/error-analysis/entity_wise_results.py data/distant_eval/unsupervised_syntax_round_2/dev.json.1000.blank.gz ~/Dropbox/SivaData/unsup_best_model_round_2/business_film_people.log.txt.eval.dev.iteration5.1best.answers.txt


spanish_bow_distant_eval:
	rm -rf ../working/spanish_bow_distant_eval
	mkdir -p ../working/spanish_bow_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-cachedKB data/freebase/domain_facts/business_film_people_facts.txt.gz \
	-lexicon data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 1 \
	-nBestTestSyntacticParses 1 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-addOnlyBagOfWordsGraph true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag true \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag true \
	-argGrelFlag false \
	-stemMatchingFlag false \
	-mediatorStemGrelPartMatchingFlag false \
	-argumentStemMatchingFlag false \
	-argumentStemGrelPartMatchingFlag false \
	-graphIsConnectedFlag false \
	-graphHasEdgeFlag true \
	-countNodesFlag false \
	-edgeNodeCountFlag false \
	-duplicateEdgesFlag true \
	-grelGrelFlag false \
	-useLexiconWeightsRel true \
	-useLexiconWeightsType true \
	-validQueryFlag true \
	-initialEdgeWeight 0.0 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -1.0 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/spanish/bow/train.json.blank.gz \
	-devFile data/distant_eval/spanish/bow/dev.json.1000.blank.gz \
	-logFile ../working/spanish_bow_distant_eval/business_film_people.log.txt \
	> ../working/spanish_bow_distant_eval/business_film_people.txt

spanish_semisup_specialcases_distant_eval:
	rm -rf ../working/spanish_semisup_specialcases_distant_eval
	mkdir -p ../working/spanish_semisup_specialcases_distant_eval
	java -Xms2048m -cp lib/*:bin in.sivareddy.graphparser.cli.RunGraphToQueryTrainingMain \
	-schema data/freebase/schema/business_film_people_schema.txt \
	-relationTypesFile lib_data/dummy.txt \
	-ccgIndexedMapping lib_data/ybisk-semi-mapping.txt \
	-ccgLexicon data/distant_eval/spanish/semisup/lexicon_fullSpecialCases.txt \
	-ccgLexiconQuestions lib_data/dummy.txt \
	-lexicon data/distant_eval/grounded_lexicon/spanish_semisup_grounded_lexicon.txt \
	-cachedKB data/freebase/domain_facts/business_facts.txt.gz \
	-binaryRules lib_data/dummy.txt \
	-unaryRules lib_data/dummy.txt \
	-domain "http://business.freebase.com;http://film.freebase.com;http://people.freebase.com" \
	-nthreads 20 \
	-timeout 3000 \
	-trainingSampleSize 1000 \
	-iterations 100 \
	-nBestTrainSyntacticParses 5 \
	-nBestTestSyntacticParses 5 \
	-nbestGraphs 100 \
	-useSchema true \
	-useKB true \
	-groundFreeVariables false \
	-useEmptyTypes false \
	-ignoreTypes false \
	-urelGrelFlag true \
	-urelPartGrelPartFlag false \
	-utypeGtypeFlag true \
	-gtypeGrelFlag false \
	-ngramGrelPartFlag false \
	-wordGrelPartFlag false \
	-wordGrelFlag false \
	-eventTypeGrelPartFlag false \
	-argGrelPartFlag false \
	-argGrelFlag false \
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
	-initialEdgeWeight -1.5 \
	-initialTypeWeight -2.0 \
	-initialWordWeight -0.05 \
	-stemFeaturesWeight 0.00 \
	-endpoint localhost \
	-trainingCorpora data/distant_eval/spanish/semisup/train.json.blank.gz \
	-devFile data/distant_eval/spanish/semisup/dev.json.1000.blank.gz \
	-logFile ../working/spanish_semisup_specialcases_distant_eval/business_film_people.log.txt \
	> ../working/spanish_semisup_specialcases_distant_eval/business_film_people.txt


create_old_to_new_mid_mappings:
	zcat kinloch:/gpfs/scratch/users/s1051585/freebase/freebase-20150720.gz | grep dataworld.gardening_hint.replaced_by | cut -f1,3 | python scripts/freebase/clean_old_mid_to_mid.py | gzip > data/freebase/freebase_20150720_old_to_new_mid.txt.gz
	zcat kinloch:/disk/scratch/users/s1051585/data/freebase-cleaned.rdf-2013-08-11-00-00.gz | grep dataworld.gardening_hint.replaced_by | cut -f1,3 | python scripts/freebase/clean_old_mid_to_mid.py | gzip > data/freebase/freebase_20130811_old_to_new_mid.txt.gz	
	zcat ../data/freebase_sempre.ttl.gz | cut -f1,3 | python scripts/freebase/extract_entities_from_rdf_freebase.py | less

extract_gold_graphs_deplambda_singletype:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge.dev \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/outputs/deplambda_singletype_with_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_lambda \
		data/gold_graphs/deplambda_singletype_with_merge.train \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
		> data/outputs/deplambda_singletype_with_merge.train.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge.dev \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/outputs/deplambda_singletype_without_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_singletype_without_merge.train \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
		> data/outputs/deplambda_singletype_without_merge.train.answers.txt

extract_gold_graphs_dependency:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_without_merge.dev \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/outputs/dependency_without_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_with_merge.dev \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
		> data/outputs/dependency_with_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_without_merge.train \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
		> data/outputs/dependency_without_merge.train.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		dependency_question_graph \
		data/gold_graphs/dependency_with_merge.train \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt \
		> data/outputs/dependency_with_merge.train.answers.txt

extract_gold_graphs_deplambda_old:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_old_without_merge.dev \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.old.json.txt \
		> data/outputs/deplambda_old_without_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
	   	dependency_lambda \
		data/gold_graphs/deplambda_old_with_merge.dev \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.old.json.txt \
		> data/outputs/deplambda_old_with_merge.dev.answers.txt

extract_gold_graphs_ccg:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge.dev \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/outputs/ccg_with_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge.dev \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/outputs/ccg_without_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_with_merge.train \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/outputs/ccg_with_merge.train.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		synPars \
		data/gold_graphs/ccg_without_merge.train \
		false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/outputs/ccg_without_merge.train.answers.txt

extract_gold_graphs_bow:
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_without_merge.dev \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/outputs/bow_without_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_with_merge.dev \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.json.txt \
		> data/outputs/bow_with_merge.dev.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_without_merge.train \
	   	false \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/outputs/bow_without_merge.train.answers.txt
	java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
   		data/freebase/schema/all_domains_schema.txt localhost \
		bow_question_graph \
		data/gold_graphs/bow_with_merge.train \
	   	true \
		< data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.json.txt \
		> data/outputs/bow_with_merge.train.answers.txt

create_deplambda_lexicon:
	cat ~/Downloads/clueweb-subset-academic-lexicon.json | sed -e "s/ u\"/ \"/g" | sed -e "s/ u'/ \"/g" | sed -e "s/{'/{\"/g" | sed -e "s/' /\" /g" | sed -e "s/':/\":/g" | sed -e "s/ '/ \"/g" | sed -e "s/',/\",/g" | java -cp lib/*:bin/ in.sivareddy.scripts.ConvertDepLambdaGroundedLexiconToGraphParser data/freebase/schema/all_domains_schema.txt > data/deplambda/clueweb-subset-academic-grounded_lexicon.txt

create_graphparser_input_from_paraphrases:
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/sampled-sentence.test.geq5.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 100 \
		> data/paraphrasing/webq.paraphrases.test.txt
	
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/sampled-sentence.train.geq5.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 100 \
		| gzip > working/webq.paraphrases.train.txt.gz

	zcat working/webq.paraphrases.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
		> data/paraphrasing/webq.paraphrases.train.txt 

	zcat working/webq.paraphrases.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		> data/paraphrasing/webq.paraphrases.dev.txt 

create_graphparser_input_from_paraphrases_5best:
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/sampled-sentence.test.geq5.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 5 \
		> data/paraphrasing/webq.paraphrases.5best.test.txt
	
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/sampled-sentence.train.geq5.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 5 \
		| gzip > working/webq.paraphrases.5best.train.txt.gz

	zcat working/webq.paraphrases.5best.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
		> data/paraphrasing/webq.paraphrases.5best.train.txt 

	zcat working/webq.paraphrases.5best.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		> data/paraphrasing/webq.paraphrases.5best.dev.txt

create_graphparser_input_from_mt_paraphrases_5best:
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/SMT-Generated/webquestions.examples.test.mt.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 5 \
		> data/paraphrasing/webq.mt.paraphrases.5best.test.txt
	
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/SMT-Generated/webquestions.examples.train.mt.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 5 \
		| gzip > working/webq.mt.paraphrases.5best.train.txt.gz

	zcat working/webq.mt.paraphrases.5best.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
		> data/paraphrasing/webq.mt.paraphrases.5best.train.txt

	zcat working/webq.mt.paraphrases.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		> data/paraphrasing/webq.mt.paraphrases.5best.dev.txt 

create_graphparser_input_from_mt_paraphrases:
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/SMT-Generated/webquestions.examples.test.mt.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.test.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 100 \
		> data/paraphrasing/webq.mt.paraphrases.test.txt
	
	mkdir -p data/paraphrasing/
	sed -e 's/,$$//g' /disk/scratch/snarayan/Siva-Data/SMT-Generated/webquestions.examples.train.mt.org-paraphrase.json \
		| grep  isOriginal \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.CreateForestFromSpectralParaphrases data/webquestions/webquestions.examples.train.domains.entity.disambiguated.3.json \
		| java -cp lib/*:bin in.sivareddy.scripts.CreateGraphParserForrestFromEntityDisambiguatedSentences \
		| java -cp lib/*:bin in.sivareddy.paraphrasing.MergeParaphrasesIntoForest 100 \
		| gzip > working/webq.mt.paraphrases.train.txt.gz

	zcat working/webq.mt.paraphrases.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.train.full.easyccg.json.txt \
		> data/paraphrasing/webq.mt.paraphrases.train.txt

	zcat working/webq.mt.paraphrases.train.txt.gz \
		| python scripts/extract_subset.py data/complete/vanilla_gold/webquestions.vanilla.dev.full.easyccg.json.txt \
		> data/paraphrasing/webq.mt.paraphrases.dev.txt 
