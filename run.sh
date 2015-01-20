# This command is for running the ungrounded semantic parser
java -cp lib/*:graph-parser.jar in.sivareddy.graphparser.cli.CcgParseToUngroundedGraphs | grep "Input Sentence\|Tokenized Sentence\|Semantic Parse"
