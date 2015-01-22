package in.sivareddy.util;

import java.util.Set;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

/**
 * String Utilities
 *
 * @author Siva Reddy
 *
 */
public class StringUtils {

  /**
   * Removes extra brackets in any string
   *
   * e.g. ((S\N))/N -> (S\N)/N
   *
   * @param inputString
   * @return
   */
  public static String removeExtraBrackets(String inputString) {
    char r[] = inputString.toCharArray();
    // char s[] = inputString.toCharArray();
    Stack<Integer> st = new Stack<Integer>();
    Set<Integer> invalidChars = Sets.newHashSet();

    int prev_start = -10;
    int prev_end = -10;
    int cur_start = -10;
    int cur_end = -10;

    for (int i = 0; i < r.length; i++) {
      if (r[i] == '(') {
        st.add(i);
        cur_start = i;
      } else if (r[i] == ')') {
        cur_end = i;
        if (cur_end - prev_end == 1 && cur_start - prev_start == 1) {
          // duplicate braces
          invalidChars.add(prev_start);
          invalidChars.add(cur_end);
        }
        Preconditions.checkArgument(st.size() > 0, "Invalid Bracketing "
            + inputString);
        cur_start = st.pop();
        if (st.size() > 0) {
          prev_start = st.peek();
        } else {
          prev_start = -10;
        }
        prev_end = cur_end;
      }
    }

    Preconditions.checkArgument(st.size() == 0, "Invalid Bracketing "
        + inputString);

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < r.length; i++) {
      if (!invalidChars.contains(i)) {
        sb.append(r[i]);
      }
    }
    return sb.toString();
  }
}
