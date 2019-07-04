package gr.uoa.di.rent.payload.responses;


import gr.uoa.di.rent.models.Profile;
import gr.uoa.di.rent.models.User;


public class UserInfo {

    private Profile profile;
    private double wallet_balance;

    public UserInfo() {
    }

    public UserInfo(Profile profile, double wallet_balance) {
        this.profile = profile;
        this.wallet_balance = wallet_balance;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public double getWallet_balance() {
        return wallet_balance;
    }

    public void setWallet_balance(double wallet_balance) {
        this.wallet_balance = wallet_balance;
    }

    public UserInfo get() {
        return this;
    }

//    public void setUser(User user) {
//        this.user = user;
//    }

}
