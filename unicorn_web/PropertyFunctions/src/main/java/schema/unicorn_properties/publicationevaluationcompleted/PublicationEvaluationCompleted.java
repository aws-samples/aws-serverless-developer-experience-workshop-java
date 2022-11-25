package schema.unicorn_properties.publicationevaluationcompleted;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.io.Serializable;

public class PublicationEvaluationCompleted implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("evaluation_result")
  private String evaluationResult = null;

  @JsonProperty("property_id")
  private String propertyId = null;

  public PublicationEvaluationCompleted evaluationResult(String evaluationResult) {
    this.evaluationResult = evaluationResult;
    return this;
  }
  

  public String getEvaluationResult() {
    return evaluationResult;
  }

  public void setEvaluationResult(String evaluationResult) {
    this.evaluationResult = evaluationResult;
  }

  public PublicationEvaluationCompleted propertyId(String propertyId) {
    this.propertyId = propertyId;
    return this;
  }
  

  public String getPropertyId() {
    return propertyId;
  }

  public void setPropertyId(String propertyId) {
    this.propertyId = propertyId;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PublicationEvaluationCompleted publicationEvaluationCompleted = (PublicationEvaluationCompleted) o;
    return Objects.equals(this.evaluationResult, publicationEvaluationCompleted.evaluationResult) &&
        Objects.equals(this.propertyId, publicationEvaluationCompleted.propertyId);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(evaluationResult, propertyId);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublicationEvaluationCompleted {\n");
    
    sb.append("    evaluationResult: ").append(toIndentedString(evaluationResult)).append("\n");
    sb.append("    propertyId: ").append(toIndentedString(propertyId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}
