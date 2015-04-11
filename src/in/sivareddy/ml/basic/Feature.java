package in.sivareddy.ml.basic;

import java.util.List;

public interface Feature {

  Double getFeatureValue();

  void setFeatureValue(Double value);

  List<?> getFeatureKey();

  @Override
  int hashCode();

  @Override
  boolean equals(Object o);

}
