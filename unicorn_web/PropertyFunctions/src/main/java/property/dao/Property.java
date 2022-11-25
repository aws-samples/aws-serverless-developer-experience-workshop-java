package property.dao;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class Property {

    String country;
    String city;
    String street;
    String propertyNumber;
    String description;
    String contract;
    Float listprice;
    String currency;
    List<String> images;
    String status;
    @JsonIgnore
    String pk;
    @JsonIgnore
    String sk;
    String id;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("PK")
    public String getPk() {
        return ("PROPERTY#" + getCountry() + "#" + getCity()).replace(' ', '-').toLowerCase();
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("SK")
    public String getSk() {
        return (getStreet() + "#" + getPropertyNumber()).replace(' ', '-').toLowerCase();
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getId() {
        return (getPk() + '/' + getSk()).replace('#', '/');
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
    public String toString() {
        return "Property [city=" + city + ", contract=" + contract + ", country=" + country + ", currency=" + currency
                + ", description=" + description + ", id=" + getId() + ", images=" + images + ", listprice=" + listprice
                + ", pk=" + getPk() + ", propertyNumber=" + propertyNumber + ", sk=" + getSk() + ", status=" + status
                + ", street=" + street + "]";
    }

}
