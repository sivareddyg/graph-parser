'''
Created on 12 Nov 2015

@author: siva
'''

import json
import re
import sys

# be.cop_arg_2(1:e , 0:x)", "what.arg_1(0:e , 0:x)", "be.cop_arg_1(1:e , 4:x)", "language(4:s , 4:x)",
# "speak.pass_arg_2(5:e , 4:x)", "QUESTION(0:x)", "UNIQUE(4:x)", "language.main(3:s , 4:x)", "speak.in.arg_2(5:e , 7:m.01_lhg)",
# "language.arg_1(4:e , 4:x)", "arg_1(7:e , 7:m.01_lhg)"

for line in sys.stdin:
    line = line.strip()
    sent = json.loads(line)
    if "dependency_lambda" in sent:
        new_parses = []
        for parse in sent['dependency_lambda']:
            parse_str = json.dumps(parse)
            if "be.cop_arg_" not in parse_str:
                continue
            new_parse = json.loads(parse_str)
            entity_matcher = re.search(
                "be\.cop_arg_2\([^\)]*:e , [^\)]*:[^x\)]*\)", parse_str)
            var_matcher = re.search(
                "be\.cop_arg_1\([^\)]*:e , [^\)]*:x\)", parse_str)
            if entity_matcher is not None and var_matcher is not None:
                entity_predicate = entity_matcher.group(0)
                var_predicate = var_matcher.group(0)
            else:
                entity_matcher = re.search(
                    "be\.cop_arg_1\([^\)]*:e , [^\)]*:[^x\)]*\)", parse_str)
                var_matcher = re.search(
                    "be\.cop_arg_2\([^\)]*:e , [^\)]*:x\)", parse_str)
                if entity_matcher is not None and var_matcher is not None:
                    entity_predicate = entity_matcher.group(0)
                    var_predicate = var_matcher.group(0)
                else:
                    entity_matcher = re.search(
                        "be\.cop_arg_1\([^\)]*:e , [^\)]*:x\)", parse_str)
                    var_matcher = re.search(
                        "be\.cop_arg_2\([^\)]*:e , [^\)]*:x\)", parse_str)
                    if entity_matcher is not None and var_matcher is not None:
                        var_predicate = var_matcher.group(0)
                        entity_predicate = entity_matcher.group(0)
                    else:
                        continue
            if not var_predicate.startswith("be.") or not entity_predicate.startswith("be."):
                continue

            predicate_var_event = re.search(
                "\((.*):e , .*\)", var_predicate).group(1)
            predicate_var = re.search(
                "\(.*:e , (.*):x\)", var_predicate).group(1)
            predicate_entity_var = re.search(
                "\(.*:e , (.*):.*\)", entity_predicate).group(1)
            predicate_entity = re.search(
                "\(.*:e , (.*)\)", entity_predicate).group(1)

            try:
                new_parse.remove(var_predicate)
                new_parse.remove(entity_predicate)
            except Exception:
                continue

            parse_str = json.dumps(new_parse)
            # print parse_str
            parse_str = parse_str.replace(
                "(%s:e" % (predicate_var_event), "(%s:e" % (predicate_entity_var))
            parse_str = parse_str.replace(
                " %s:e" % (predicate_var_event), " %s:e" % (predicate_entity_var))
            parse_str = parse_str.replace(
                "(%s:e" % (predicate_var), "(%s:e" % (predicate_entity_var))
            parse_str = parse_str.replace(
                " %s:e" % (predicate_var), " %s:e" % (predicate_entity_var))
            parse_str = parse_str.replace(
                "(%s:x" % (predicate_var), "(%s" % (predicate_entity))
            parse_str = parse_str.replace(
                " %s:x" % (predicate_var), " %s" % (predicate_entity))
            new_parse = json.loads(parse_str)
            new_parses.append(new_parse)

            # print parse
            # print new_parse
        sent['dependency_lambda'].extend(new_parses)
        print json.dumps(sent)
