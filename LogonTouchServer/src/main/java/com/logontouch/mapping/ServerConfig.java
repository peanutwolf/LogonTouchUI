
package com.logontouch.mapping;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ServerConfig {

    @SerializedName("version")
    @Expose
    private String version;
    @SerializedName("HTTPPort")
    @Expose
    private Integer hTTPPort;
    @SerializedName("HTTPSPort")
    @Expose
    private Integer hTTPSPort;
    @SerializedName("KeysDir")
    @Expose
    private KeysDir keysDir = new KeysDir();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Integer getHTTPPort() {
        return hTTPPort;
    }

    public void setHTTPPort(Integer hTTPPort) {
        this.hTTPPort = hTTPPort;
    }

    public Integer getHTTPSPort() {
        return hTTPSPort;
    }

    public void setHTTPSPort(Integer hTTPSPort) {
        this.hTTPSPort = hTTPSPort;
    }

    public KeysDir getKeysDir() {
        return keysDir;
    }

    public void setKeysDir(KeysDir keysDir) {
        this.keysDir = keysDir;
    }

    public String getKeysDirFolder(){
        return this.keysDir.getPath();
    }

    public String getClientKeysDirFolder(){
        return this.keysDir.getClientKeysDir().getPath();
    }

    public String getServerKeysDirFolder(){
        return this.keysDir.getServerKeysDir().getPath();
    }

    public String getClientPublicStore(){
        return this.keysDir.getClientKeysDir().getPublicStore();
    }

    public String getClientPublicPass(){
        return this.keysDir.getClientKeysDir().getPublicPass();
    }

    public String getClientCredentials(){
        return this.keysDir.getClientKeysDir().getCredentials();
    }

    public String getServerPublicStore(){
        return this.keysDir.getServerKeysDir().getPublicStore();
    }

    public String getServerPublicPass(){
        return this.keysDir.getServerKeysDir().getPublicPass();
    }

    public String getServerPrivateStore(){
        return this.keysDir.getServerKeysDir().getPrivateStore();
    }

    public String getServerPrivatePass(){
        return this.keysDir.getServerKeysDir().getPrivatePass();
    }

}
