package in.sivareddy.util;

import java.util.regex.Pattern;

public class Crap {

  public static void main(String[] args) {
    String s = "($y $y*)";
    String y = Pattern.quote("$x");
    System.out.println(y);
    String k = s.replaceAll(String.format("([\\s\\)\\(]+)(%s)([\\s\\)\\(]+)", Pattern.quote("$y")),
        String.format("%s%s%s", "$1", "$x", "$3"));
    System.out.println(k);
  }
}
