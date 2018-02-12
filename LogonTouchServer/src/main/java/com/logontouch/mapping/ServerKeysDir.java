
package com.logontouch.mapping;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ServerKeysDir {

    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("PrivateStore")
    @Expose
    private String privateStore;
    @SerializedName("PrivatePass")
    @Expose
    private String privatePass;
    @SerializedName("PublicStore")
    @Expose
    private String publicStore;
    @SerializedName("PublicPass")
    @Expose
    private String publicPass;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPrivateStore() {
        return privateStore;
    }

    public void setPrivateStore(String privateStore) {
        this.privateStore = privateStore;
    }

    public String getPrivatePass() {
        return privatePass;
    }

    public void setPrivatePass(String privatePass) {
        this.privatePass = privatePass;
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

}
