
package com.logontouch.mapping;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class KeysDir {

    @SerializedName("path")
    @Expose
    private String path;
    @SerializedName("ServerKeysDir")
    @Expose
    private ServerKeysDir serverKeysDir = new ServerKeysDir();
    @SerializedName("ClientKeysDir")
    @Expose
    private ClientKeysDir clientKeysDir = new ClientKeysDir();

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public ServerKeysDir getServerKeysDir() {
        return serverKeysDir;
    }

    public void setServerKeysDir(ServerKeysDir serverKeysDir) {
        this.serverKeysDir = serverKeysDir;
    }

    public ClientKeysDir getClientKeysDir() {
        return clientKeysDir;
    }

    public void setClientKeysDir(ClientKeysDir clientKeysDir) {
        this.clientKeysDir = clientKeysDir;
    }

}
