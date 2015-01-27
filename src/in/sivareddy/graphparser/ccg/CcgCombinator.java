package in.sivareddy.graphparser.ccg;

import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

/**
 * Created by bisk1 on 1/26/15.
 */
public enum CcgCombinator {
  conj, tr, lex, fa, ba, gfc, gbc, gbx, fc, bc, bx, lp, rp, ltc, rtc, other;

  public static Map<String, CcgCombinator> map;

  static {
    Map<String, CcgCombinator> mutMap = Maps.newHashMap();
    mutMap.put("conj", conj);
    mutMap.put("tr", tr);
    mutMap.put("lex", lex);
    mutMap.put("fa", fa);
    mutMap.put("ba", ba);
    mutMap.put("gfc", gfc);
    mutMap.put("gbc", gbc);
    mutMap.put("gbx", gbx);
    mutMap.put("fc", fc);
    mutMap.put("bc", bc);
    mutMap.put("bx", bx);
    mutMap.put("lp", lp);
    mutMap.put("rp", rp);
    mutMap.put("ltc", ltc);
    mutMap.put("rtc", rtc);
    map = Collections.unmodifiableMap(mutMap);
  }

  public static CcgCombinator getCombinator(String combinator) {
    if (map.containsKey(combinator))
      return map.get(combinator);
    else
      return other;
  }

  public final static ImmutableSet<String> types = ImmutableSet.of("conj",
      "tr", "lex", "fa", "ba", "gfc", "gbc", "gbx", "fc", "bc", "bx", "lp",
      "rp", "ltc", "rtc", "other");
}
