package net.unit8.bouncr.hook.license;

import java.io.Serializable;

public class LicenseConfig implements Serializable {
    private int numOfDevicesPerUser = 3;
    private String cookieName = "BOUNCR_LICENSE_KEY";

    public int getNumOfDevicesPerUser() {
        return numOfDevicesPerUser;
    }

    public void setNumOfDevicesPerUser(int numOfDevicesPerUser) {
        this.numOfDevicesPerUser = numOfDevicesPerUser;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }
}
