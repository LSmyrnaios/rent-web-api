package gr.uoa.di.rent.payload.responses;

import gr.uoa.di.rent.models.Business;


public class BusinessResponse {

    private Business business;

    public BusinessResponse() {
    }

    public BusinessResponse(Business business) {
        this.business = business;
    }

    public Business getBusiness() {
        return business;
    }

    public void setBusiness(Business business) {
        this.business = business;
    }

    @Override
    public String toString() {
        return "BusinessResponse{" +
                "business=" + business +
                '}';
    }
}
