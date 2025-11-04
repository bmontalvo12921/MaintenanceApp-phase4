public class Customer {
    private String phoneNumber;
    private String name;
    private String address;
    private String email;

    public Customer(String phoneNumber, String name, String address, String email) {
        this.phoneNumber = phoneNumber;
        this.name = name;
        this.address = address;
        this.email = email;
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public String getEmail() { return email; }

    public void setPhoneNumber(String v) { phoneNumber = v; }
    public void setName(String v) { name = v; }
    public void setAddress(String v) { address = v; }
    public void setEmail(String v) { email = v; }
}
