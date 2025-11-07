package dao;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class Property {

    private String country;
    private String city;
    private String street;
    private String propertyNumber;
    private String description;
    private String contract;
    private Float listprice;
    private String currency;
    private List<String> images;
    private String status;
    @JsonIgnore
    private String pk;
    @JsonIgnore
    private String sk;
    private String id;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        if (country == null || city == null) {
            return pk; // Return stored value if components are null
        }
        return ("PROPERTY#" + country.toLowerCase() + "#" + city.toLowerCase()).replace(' ', '-');
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        if (street == null || propertyNumber == null) {
            return sk; // Return stored value if components are null
        }
        return (street + "#" + propertyNumber).replace(' ', '-').toLowerCase();
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    @JsonIgnore
    @software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnore
    public String getId() {
        if (id != null) {
            return id;
        }
        String partitionKey = getPk();
        String sortKey = getSk();
        if (partitionKey != null && sortKey != null) {
            return (partitionKey + '/' + sortKey).replace('#', '/');
        }
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    @DynamoDbAttribute(value = "number")
    public String getPropertyNumber() {
        return propertyNumber;
    }

    public void setPropertyNumber(String propertyNumber) {
        this.propertyNumber = propertyNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public Float getListprice() {
        return listprice;
    }

    public void setListprice(Float listprice) {
        this.listprice = listprice;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Property property = (Property) o;
        return Objects.equals(getId(), property.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public String toString() {
        return "Property{" +
                "country='" + country + '\'' +
                ", city='" + city + '\'' +
                ", street='" + street + '\'' +
                ", propertyNumber='" + propertyNumber + '\'' +
                ", status='" + status + '\'' +
                ", listprice=" + listprice +
                ", currency='" + currency + '\'' +
                '}';
    }
}
