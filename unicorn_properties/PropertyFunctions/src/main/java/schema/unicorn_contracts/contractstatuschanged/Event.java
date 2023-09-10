package schema.unicorn_contracts.contractstatuschanged;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import schema.unicorn_contracts.contractstatuschanged.ContractStatusChanged;
import java.io.Serializable;

public class Event implements Serializable {
  private static final long serialVersionUID = 1L;

  @JsonProperty("account")
  private String account = null;

  @JsonProperty("detail")
  private ContractStatusChanged detail = null;

  @JsonProperty("detail-type")
  private String detailType = null;

  @JsonProperty("id")
  private String id = null;

  @JsonProperty("region")
  private String region = null;

  @JsonProperty("resources")
  private List<Object> resources = null;

  @JsonProperty("source")
  private String source = null;

  @JsonProperty("time")
  private Date time = null;

  @JsonProperty("version")
  private String version = null;

  public Event account(String account) {
    this.account = account;
    return this;
  }
  

  public String getAccount() {
    return account;
  }

  public void setAccount(String account) {
    this.account = account;
  }

  public Event detail(ContractStatusChanged detail) {
    this.detail = detail;
    return this;
  }
  

  public ContractStatusChanged getDetail() {
    return detail;
  }

  public void setDetail(ContractStatusChanged detail) {
    this.detail = detail;
  }

  public Event detailType(String detailType) {
    this.detailType = detailType;
    return this;
  }
  

  public String getDetailType() {
    return detailType;
  }

  public void setDetailType(String detailType) {
    this.detailType = detailType;
  }

  public Event id(String id) {
    this.id = id;
    return this;
  }
  

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Event region(String region) {
    this.region = region;
    return this;
  }
  

  public String getRegion() {
    return region;
  }

  public void setRegion(String region) {
    this.region = region;
  }

  public Event resources(List<Object> resources) {
    this.resources = resources;
    return this;
  }
  public Event addResourcesItem(Object resourcesItem) {
    if (this.resources == null) {
      this.resources = new ArrayList<Object>();
    }
    this.resources.add(resourcesItem);
    return this;
  }

  public List<Object> getResources() {
    return resources;
  }

  public void setResources(List<Object> resources) {
    this.resources = resources;
  }

  public Event source(String source) {
    this.source = source;
    return this;
  }
  

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public Event time(Date time) {
    this.time = time;
    return this;
  }
  

  public Date getTime() {
    return time;
  }

  public void setTime(Date time) {
    this.time = time;
  }

  public Event version(String version) {
    this.version = version;
    return this;
  }
  

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Event event = (Event) o;
    return Objects.equals(this.account, event.account) &&
        Objects.equals(this.detail, event.detail) &&
        Objects.equals(this.detailType, event.detailType) &&
        Objects.equals(this.id, event.id) &&
        Objects.equals(this.region, event.region) &&
        Objects.equals(this.resources, event.resources) &&
        Objects.equals(this.source, event.source) &&
        Objects.equals(this.time, event.time) &&
        Objects.equals(this.version, event.version);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(account, detail, detailType, id, region, resources, source, time, version);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class Event {\n");
    
    sb.append("    account: ").append(toIndentedString(account)).append("\n");
    sb.append("    detail: ").append(toIndentedString(detail)).append("\n");
    sb.append("    detailType: ").append(toIndentedString(detailType)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    region: ").append(toIndentedString(region)).append("\n");
    sb.append("    resources: ").append(toIndentedString(resources)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
    sb.append("    time: ").append(toIndentedString(time)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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
