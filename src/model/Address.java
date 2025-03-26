package src.model;

public class Address {
    private String addressId;
    private String street;
    private String postalCode;
    private String city;

    private String countryId;

    public Address() {
    }

    public Address(String addressId, String street, String postalCode, String city, String countryId) {
        this.addressId = addressId;
        this.street = street;
        this.postalCode = postalCode;
        this.city = city;
        this.countryId = countryId;
    }

    public String getAddressId() {
        return addressId;
    }

    public void setAddressId(String addressId) {
        this.addressId = addressId;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    @Override
    public String toString() {
        return "Address{" +
                "addressId='" + addressId + '\'' +
                ", street='" + street + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", city='" + city + '\'' +
                ", countryId='" + countryId + '\'' +
                '}';
    }
}
