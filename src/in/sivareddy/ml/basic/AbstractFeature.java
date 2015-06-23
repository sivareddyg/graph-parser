package in.sivareddy.ml.basic;

import java.io.Serializable;
import java.util.List;

import com.google.common.base.Objects;

public abstract class AbstractFeature implements Feature, Comparable<Feature>,
    Serializable {
  private static final long serialVersionUID = -7471409940498505373L;
  private final List<?> key;
  private Double value;
  private static final int prime = 31;

  public AbstractFeature(List<?> key, Double value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public Double getFeatureValue() {
    return value;
  }

  @Override
  public void setFeatureValue(Double value) {
    this.value = value;
  }

  @Override
  public List<?> getFeatureKey() {
    return key;
  }

  @Override
  public int hashCode() {
    return getClass().getName().hashCode() * (prime + key.hashCode());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!obj.getClass().equals(getClass())) {
      return false;
    }
    AbstractFeature other = (AbstractFeature) obj;
    return other.key.equals(key);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this).addValue(key).addValue(value)
        .toString();
  }

  @Override
  public int compareTo(Feature o) {
    return key.toString().compareTo(o.getFeatureKey().toString());
  }

}
