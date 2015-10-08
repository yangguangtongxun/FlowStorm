package com.hnxy.hxy.app.flowstorm.ui;

import java.io.Serializable;

import de.blinkt.openvpn.VpnProfile;


/**
 * Created by hu60 on 15-8-10.
 */
public class Account implements Serializable {
    public String mUsername;
    public String mPassword;
    public transient String mTransientPW;
    public transient String mTransientPCKS12PW;

    public void updateProfile(VpnProfile profile) {
        profile.mUsername = mUsername;
        profile.mPassword = mPassword;
        profile.mTransientPW = mTransientPW;
        profile.mTransientPCKS12PW = mTransientPCKS12PW;
    }
}
