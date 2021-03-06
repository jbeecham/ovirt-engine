package org.ovirt.engine.core.common.queries;

public enum ConfigurationValues {
    MaxNumOfVmCpus(ConfigAuthType.User),
    MaxNumOfVmSockets(ConfigAuthType.User),
    MaxNumOfCpuPerSocket(ConfigAuthType.User),
    VirtualMachineDomainName,
    VdcVersion(ConfigAuthType.User),
    // GetAllAdDomains,
    SSLEnabled(ConfigAuthType.User),
    CipherSuite(ConfigAuthType.User),
    VmPoolLeaseDays(ConfigAuthType.User),
    VmPoolLeaseStartTime(ConfigAuthType.User),
    VmPoolLeaseEndTime(ConfigAuthType.User),
    MaxVmsInPool(ConfigAuthType.User),
    MaxVdsMemOverCommit(ConfigAuthType.User),
    MaxVdsMemOverCommitForServers(ConfigAuthType.User),
    AdUserName,
    // TODO remove remarks and AdUserPassword completely in version 3.1.
    // AdUserPassword field format has been changed.
    // AdUserPassword,
    LocalAdminPassword,
    ValidNumOfMonitors(ConfigAuthType.User),
    EnableUSBAsDefault(ConfigAuthType.User),
    SpiceSecureChannels(ConfigAuthType.User),
    SpiceReleaseCursorKeys(ConfigAuthType.User),
    SpiceToggleFullScreenKeys(ConfigAuthType.User),
    HighUtilizationForEvenlyDistribute(ConfigAuthType.User),
    RDPLoginWithFQN,
    SpiceUsbAutoShare(ConfigAuthType.User),
    ImportDefaultPath,
    ComputerADPaths(ConfigAuthType.User),
    PowerClientGUI,
    VdsSelectionAlgorithm,
    LowUtilizationForEvenlyDistribute,
    LowUtilizationForPowerSave,
    HighUtilizationForPowerSave,
    CpuOverCommitDurationMinutes,
    InstallVds,
    AsyncTaskPollingRate,
    VdsFenceType,
    VdsFenceOptions,
    VdsFenceOptionMapping,
    VcpuConsumptionPercentage(ConfigAuthType.User),
    CertificateFingerPrint,
    SearchResultsLimit(ConfigAuthType.User),
    MaxBlockDiskSize(ConfigAuthType.User),
    RedirectServletReportsPage(ConfigAuthType.User),
    RedirectServletReportsPageError(ConfigAuthType.User),
    EnableSpiceRootCertificateValidation(ConfigAuthType.User),
    VMMinMemorySizeInMB(ConfigAuthType.User),
    VM32BitMaxMemorySizeInMB(ConfigAuthType.User),
    VM64BitMaxMemorySizeInMB(ConfigAuthType.User),
    VmPriorityMaxValue(ConfigAuthType.User),
    StorageDomainNameSizeLimit(ConfigAuthType.User),
    StoragePoolNameSizeLimit(ConfigAuthType.User),
    SANWipeAfterDelete(ConfigAuthType.User),
    AuthenticationMethod(ConfigAuthType.User),
    LocalStorageEnabled,
    UserDefinedVMProperties(ConfigAuthType.User),
    PredefinedVMProperties(ConfigAuthType.User),
    SupportCustomProperties(ConfigAuthType.User),
    VdsFenceOptionTypes,
    ServerCPUList,
    SupportedClusterLevels(ConfigAuthType.User),
    ProductRPMVersion(ConfigAuthType.User),
    RhevhLocalFSPath,
    CustomPublicConfig_AppsWebSite(ConfigAuthType.User),
    DocsURL(ConfigAuthType.User),
    HotPlugEnabled(ConfigAuthType.User),
    SupportBridgesReportByVDSM(ConfigAuthType.User),
    ManagementNetwork,
    ApplicationMode(ConfigAuthType.User),
    ShareableDiskEnabled(ConfigAuthType.User),
    DirectLUNDiskEnabled(ConfigAuthType.User),
    LiveStorageMigrationEnabled(ConfigAuthType.User),
    WANDisableEffects(ConfigAuthType.User),
    WANColorDepth(ConfigAuthType.User),
    SupportForceCreateVG,
    NetworkConnectivityCheckTimeoutInSeconds,
    AllowClusterWithVirtGlusterEnabled,
    MTUOverrideSupported(ConfigAuthType.User),
    GlusterVolumeOptionGroupVirtValue,
    GlusterVolumeOptionOwnerUserVirtValue,
    GlusterVolumeOptionOwnerGroupVirtValue;

    public static enum ConfigAuthType {
        Admin,
        User
    }

    private ConfigAuthType authType;

    private ConfigurationValues(ConfigAuthType authType) {
        this.authType = authType;
    }

    private ConfigurationValues() {
        this(ConfigAuthType.Admin);
    }

    public ConfigAuthType getConfigAuthType() {
        return authType;
    }

    public boolean isAdmin() {
        return ConfigAuthType.Admin == authType;
    }

    public int getValue() {
        return ordinal();
    }

    public static ConfigurationValues forValue(int value) {
        return values()[value];
    }
}
