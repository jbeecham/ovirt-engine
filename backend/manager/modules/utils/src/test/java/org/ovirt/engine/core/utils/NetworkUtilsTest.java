package org.ovirt.engine.core.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.ovirt.engine.core.common.businessentities.Network;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface;
import org.ovirt.engine.core.common.businessentities.VdsNetworkInterface.NetworkImplementationDetails;

public class NetworkUtilsTest {

    @Rule
    public static RandomUtilsSeedingRule rusr = new RandomUtilsSeedingRule();

    @Test
    public void calculateNetworkImplementationDetailsUnmamagedNetwork() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();

        assertNull("Network implementation details should not be filled.",
                NetworkUtils.calculateNetworkImplementationDetails(Collections.<String, Network> emptyMap(), iface));
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkIsSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                true,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkIsSyncWithMtuUnset() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                true,
                iface.getNetworkName(),
                iface.isBridged(),
                0,
                iface.getVlanId());
    }

    /**
     * Cover a case when MTU is unset & other network parameters out of cync, which is not covered by other tests.
     */
    @Test
    public void calculateNetworkImplementationDetailsNetworkOutOfSyncWithMtuUnset() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                !iface.isBridged(),
                0,
                RandomUtils.instance().nextInt());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkMtuOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu() + 1,
                iface.getVlanId());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkVmNetworkOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                !iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId());
    }

    @Test
    public void calculateNetworkImplementationDetailsNetworkVlanOutOfSync() throws Exception {
        VdsNetworkInterface iface = createNetworkDevice();
        calculateNetworkImplementationDetailsAndAssertSync(iface,
                false,
                iface.getNetworkName(),
                iface.isBridged(),
                iface.getMtu(),
                iface.getVlanId() + 1);
    }

    private void calculateNetworkImplementationDetailsAndAssertSync(VdsNetworkInterface iface,
            boolean expectSync,
            String networkName,
            boolean vmNet,
            int mtu, int vlanId) {
        Map<String, Network> networks = createNetworksMap(networkName, vmNet, mtu, vlanId);

        NetworkImplementationDetails networkImplementationDetails =
                NetworkUtils.calculateNetworkImplementationDetails(networks, iface);

        assertNotNull("Network implementation details should be filled.", networkImplementationDetails);
        assertEquals("Network implementation details should be " + (expectSync ? "in" : "out of") + " sync.",
                expectSync,
                networkImplementationDetails.isInSync());
    }

    private Map<String, Network> createNetworksMap(String networkName,
            boolean vmNetwork,
            int mtu,
            Integer vlanId) {
        Network network = new Network();
        network.setVmNetwork(vmNetwork);
        network.setMtu(mtu);
        network.setvlan_id(vlanId);

        return Collections.singletonMap(networkName, network);
    }

    private VdsNetworkInterface createNetworkDevice() {
        VdsNetworkInterface iface = new VdsNetworkInterface();
        iface.setNetworkName(RandomUtils.instance().nextString(10));
        iface.setBridged(RandomUtils.instance().nextBoolean());
        iface.setMtu(100);
        iface.setVlanId(100);
        return iface;
    }
}