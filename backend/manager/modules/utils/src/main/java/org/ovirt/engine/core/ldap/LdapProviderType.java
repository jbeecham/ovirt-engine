package org.ovirt.engine.core.ldap;

public enum LdapProviderType {
    activeDirectory("Microsoft Active Directory"),
    ipa("389 Project"),
    rhds("Red Hat"),
    itds("IBM Tivoli Directory Server"),
    general("Deprecated - for auto-detection usages"); // for rootDSE purpose

    private String vendorName;

    private LdapProviderType(String vendorName) {
        this.vendorName = vendorName;
    }

    public String getLdapVendorName() {
        return vendorName;
    }
    public static LdapProviderType valueOfIgnoreCase(String name) {
        if (name == null) {
            throw new NullPointerException("Name is null");
        }
        for (LdapProviderType type : values()) {
            if (name.equalsIgnoreCase(type.name())) {
                return type;
            }
        }
        throw new IllegalArgumentException("No enum const for name " + name);
    }

}
