
package com.logontouch.mapping;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientKeysDir {

    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("PublicStore")
    @Expose
    private String publicStore;
    @SerializedName("PublicPass")
    @Expose
    private String publicPass;
    @SerializedName("Credentials")
    @Expose
    private String credentials;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPublicStore() {
        return publicStore;
    }

    public void setPublicStore(String publicStore) {
        this.publicStore = publicStore;
    }

    public String getPublicPass() {
        return publicPass;
    }

    public void setPublicPass(String publicPass) {
        this.publicPass = publicPass;
    }

    public String getCredentials() {
        return credentials;
    }

    public void setCredentials(String credentials) {
        this.credentials = credentials;
    }

}
