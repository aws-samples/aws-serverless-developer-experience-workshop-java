package contracts.utils;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class Address {

    @JsonProperty("country")
    private String country;
    
    @JsonProperty("city")
    private String city;
    
    @JsonProperty("street")
    private String street;
    
    @JsonProperty("number")
    private int number;

    public Address() {}

    public Address(String country, String city, String street, int number) {
        this.country = country;
        this.city = city;
        this.street = street;
        this.number = number;
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

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return number == address.number &&
               Objects.equals(country, address.country) &&
               Objects.equals(city, address.city) &&
               Objects.equals(street, address.street);
    }

    @Override
    public int hashCode() {
        return Objects.hash(country, city, street, number);
    }

    @Override
    public String toString() {
        return "Address{" +
               "country='" + country + '\'' +
               ", city='" + city + '\'' +
               ", street='" + street + '\'' +
               ", number=" + number +
               '}';
    }
}
