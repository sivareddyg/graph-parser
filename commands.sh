cat data/complete/vanilla_automatic/webquestions.automaticDismabiguation.train.pass3.deplambda.singletype.json.txt data/complete/vanilla_automatic/webquestions.automaticDismabiguation.dev.pass3.deplambda.singletype.json.txt \
    | java -cp lib/*:bin/ in.sivareddy.scripts.EvaluateGraphParserOracleUsingGoldMidAndGoldRelations \
        data/freebase/schema/all_domains_schema.txt localhost \
        dependency_lambda \
        data/gold_graphs/deplambda_singletype_with_merge.full \
        lib_data/dummy.txt \
        true \
        > data/gold_graphs/deplambda_singletype_with_merge.full.answers.txt
make deplambda_singletype_supervised_with_merge
