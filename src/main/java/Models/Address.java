package Models;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import Utils.JsonUtils;

public class Address {
    private ObjectId addressId;
    private String number;
    private String street;
    private String zipCode;
    private String city;
    private String countryId;

    public Address() {
    }

    public Address(String addressId, String street, String number, String postalCode, String city, String countryId) {
        this.addressId = new ObjectId(addressId); //.toHexString();
        this.number = number;
        this.street = street;
        this.zipCode = postalCode;
        this.city = city;
        this.countryId = countryId;
    }

    // json constructor
    public Address(JSONObject json) {
        System.out.println("DEBUG : " + json.toString());
        this.addressId = JsonUtils.getObjectId(json, "addressId");
        this.number = json.optString("number", "");
        this.street = json.getString("street");
        this.zipCode = json.getString("zipCode");
        this.city = json.getString("city");
        this.countryId = json.getString("country");
    }

    public ObjectId getAddressId() {
        return addressId;
    }

    public void setAddressId(ObjectId addressId) {
        this.addressId = addressId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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
       JSONObject json = this.toJSON();
        return json.toString();
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("addressId", addressId);
        jsonObject.put("number", number);
        jsonObject.put("street", street);
        jsonObject.put("zipCode", zipCode);
        jsonObject.put("city", city);
        jsonObject.put("country", countryId);
        return jsonObject;
    }

}
