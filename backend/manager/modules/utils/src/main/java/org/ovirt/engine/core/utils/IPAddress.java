package org.ovirt.engine.core.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class IPAddress {

    private byte[] ip;

    public IPAddress(byte[] ipAddr) {
        ip = ipAddr;
    }

    public byte[] GetAddressBytes() {
        return ip;
    }

    public static IPAddress Parse(String ipString) {
        try {
            return new IPAddress(InetAddress.getByName(ipString).getAddress());
        } catch (UnknownHostException ex) {
            RuntimeException newEx = new RuntimeException(ex.getMessage());
            newEx.setStackTrace(ex.getStackTrace());
            throw newEx;
        }
    }

    @Override
    public String toString() {
        if (ip == null) {
            return "";
        }
        try {
            return InetAddress.getByAddress(ip).getHostAddress();
        } catch (UnknownHostException ex) {
            RuntimeException newEx = new RuntimeException(ex.getMessage());
            newEx.setStackTrace(ex.getStackTrace());
            throw newEx;
        }
    }

}
