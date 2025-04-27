package model;

import org.bson.types.ObjectId;
import org.json.JSONObject;
import util.JsonUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Investor {
    private ObjectId investorId;
    private String username;
    private String password;
    private String name;
    private String surname;
    private String email;
    private String phoneNumber;
    private LocalDateTime creationDate;
    private LocalDateTime lastUpdateDate;
    private List<Wallet> wallets;
    private List<Address> addresses;

    public Investor() {
    }

    public Investor(ObjectId investorId, String username, String password, String name, String surname, String email,
                    String phoneNumber, LocalDateTime creationDate, LocalDateTime lastUpdateDate,
                    List<Wallet> wallets, List<Address> addresses) {
        this.investorId = investorId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.surname = surname;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.creationDate = creationDate;
        this.lastUpdateDate = lastUpdateDate;
        this.wallets = wallets;
        this.addresses = addresses;
    }

    // investor from json
    public Investor(JSONObject json) {
        // Required fields
        if (!json.has("username") || !json.has("password") || !json.has("name") ||
                !json.has("surname") || !json.has("email") || !json.has("phoneNumber")) {
            throw new IllegalArgumentException("Missing required fields in JSON input." +
                    " Required fields: " +
                    "String username, String password, String name, String surname, " +
                    "String email, String phoneNumber");
        }

        // Ensure at least one address provided
        if (!json.has("addresses") || json.isNull("addresses") || json.getJSONArray("addresses").length() == 0) {
            throw new IllegalArgumentException("At least one address is required.");
        }

        // Set required fields
        this.investorId = JsonUtils.getObjectId(json, "investorId");  // Optional returns a new ObjectId if missing
        this.username = json.getString("username");
        this.password = json.getString("password");
        this.name = json.getString("name");
        this.surname = json.getString("surname");
        this.email = json.getString("email");
        this.phoneNumber = json.getString("phoneNumber");

        // Optional fields with fallback if not present
        Object creationDateObj = json.opt("creationDate");
        this.creationDate = (creationDateObj instanceof LocalDateTime) ?
                (LocalDateTime) creationDateObj :
                (creationDateObj instanceof String) ?
                        LocalDateTime.parse((String) creationDateObj) :
                        null;

        Object lastUpdateDateObj = json.opt("lastUpdateDate");
        this.lastUpdateDate = (lastUpdateDateObj instanceof LocalDateTime) ?
                (LocalDateTime) lastUpdateDateObj :
                (lastUpdateDateObj instanceof String) ?
                        LocalDateTime.parse((String) lastUpdateDateObj) :
                        null;

        // Optional wallets field
        this.wallets = json.has("wallets") && !json.isNull("wallets") ?
                json.getJSONArray("wallets").toList().stream()
                        .map(obj -> {
                            try {
                                JSONObject walletJson = new JSONObject((Map) obj); // Convert Map to JSONObject
                                return new Wallet(walletJson);
                            } catch (Exception e) {
                                throw new IllegalArgumentException("wallet : " + obj + " has an invalid format"+e);
                            }
                        })
                        .collect(Collectors.toList()) : new ArrayList<>();


        // Convert addresses to Address objects
        this.addresses = json.getJSONArray("addresses").toList().stream()
                .map(obj -> {
                    System.out.println("Type of address object: " + obj.getClass().getName());
                    try {
                        JSONObject addressJson = new JSONObject((Map) obj);
                        return new Address(addressJson);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("address : " + obj + " has an invalid format -" + e);
                    }
                })
                .collect(Collectors.toList());
    }

    public ObjectId getInvestorId() {
        return investorId;
    }

    public void setInvestorId(ObjectId investorId) {
        this.investorId = investorId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getLastUpdateDate() {
        return lastUpdateDate;
    }

    public void setLastUpdateDate(LocalDateTime lastUpdateDate) {
        this.lastUpdateDate = lastUpdateDate;
    }

    public List<Wallet> getWallets() {
        return wallets;
    }

    public void setWallets(List<Wallet> wallets) {
        this.wallets = wallets;
    }

    public List<Address> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<Address> addresses) {
        this.addresses = addresses;
    }

    @Override
    public String toString() {
        // to json
        JSONObject json = this.toJson();
        return json.toString();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("investorId", investorId);
        json.put("username", username);
        json.put("password", password);
        json.put("name", name);
        json.put("surname", surname);
        json.put("email", email);
        json.put("phoneNumber", phoneNumber);
        json.put("creationDate", creationDate); //.toString());
        json.put("lastUpdateDate", lastUpdateDate); //.toString());

        // Convert wallets to JSON array
        for (Wallet wallet : wallets) {
            json.append("wallets", wallet.toJson());
        }

        // Convert addresses to JSON array
        for (Address address : addresses) {
            json.append("addresses", address.toJSON());
        }
        return json;
    }

}
