package src.dao;


public interface AddresseDAO {
    public void addAddress(String address);
    public void deleteAddress(String address);
    public void updateAddress(String address);
    public void getAddress(String address);
    public void getAllAddresses();
}
