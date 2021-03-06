package org.ovirt.engine.core.bll.gluster;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AccessControlException;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.ovirt.engine.core.common.config.Config;
import org.ovirt.engine.core.common.config.ConfigValues;
import org.ovirt.engine.core.common.queries.gluster.GlusterServersQueryParameters;
import org.ovirt.engine.core.engineencryptutils.OpenSSHUtils;
import org.ovirt.engine.core.utils.XmlUtils;
import org.ovirt.engine.core.utils.ssh.ConstraintByteArrayOutputStream;
import org.ovirt.engine.core.utils.ssh.SSHClient;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Query to fetch list of gluster servers via ssh using the given serverName and password.
 *
 * This query will be invoked from Import Gluster Cluster dialog. In the dialog the user will provide the servername and
 * password of any one of the server in the cluster. Since, the importing cluster haven't been bootstarped yet, we are
 * running the gluster peer status command via ssh.
 *
 */
public class GetGlusterServersQuery<P extends GlusterServersQueryParameters> extends GlusterQueriesCommandBase<P> {

    private static final String PEER = "peer";
    private static final String HOST_NAME = "hostname";
    private static final String STATE = "state";
    private static final int PEER_IN_CLUSTER = 3;
    private static final int PORT = 22;
    private static final String ROOT = "root";

    public GetGlusterServersQuery(P parameters) {
        super(parameters);
    }

    @Override
    protected void executeQueryCommand() {
        SSHClient client = null;

        try {
            client = connect(getParameters().getServerName());
            validateFingerprint(client, getParameters().getFingerprint());
            authenticate(client, ROOT, getParameters().getPassword());
            String serversXml = executeCommand(client);
            getQueryReturnValue().setReturnValue(extractServers(serversXml));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (client != null) {
                client.disconnect();
            }
        }
    }

    protected Map<String, String> extractServers(String serversXml) throws ParserConfigurationException,
            SAXException, IOException {
        if (StringUtils.isNotEmpty(serversXml)) {
            return getServerFingerprints(XmlUtils.loadXmlDoc(serversXml).getElementsByTagName(PEER));
        } else {
            throw new RuntimeException("Could not get the peer list form the host: " + getParameters().getServerName());
        }
    }

    private Map<String, String> getServerFingerprints(NodeList listOfPeers) {
        Map<String, String> fingerprints = new HashMap<String, String>();
        // Add current server finger print also in the map
        fingerprints.put(getParameters().getServerName(), getParameters().getFingerprint());
        for (int i = 0; i < listOfPeers.getLength(); i++) {
            Node firstPeer = listOfPeers.item(i);
            if (firstPeer.getNodeType() == Node.ELEMENT_NODE) {
                Element firstHostElement = (Element) firstPeer;
                int state = XmlUtils.getIntValue(firstHostElement, STATE);
                // Add the server only if the state is 3
                if (state == PEER_IN_CLUSTER) {
                    String hostName = XmlUtils.getTextValue(firstHostElement, HOST_NAME);
                    fingerprints.put(hostName, getFingerprint(hostName));
                }
            }
        }
        return fingerprints;
    }

    protected void validateFingerprint(SSHClient client, String fingerprint) {
        if (!fingerprint.equals(getFingerprint(client))) {
            throw new AccessControlException(
                    String.format(
                            "SSH Fingerprint of server '%1$s' did not match expected fingerprint '%2$s'",
                            client.getDisplayHost(),
                            fingerprint
                            ));
        }
    }

    // This is required for mock during Junit test
    protected SSHClient createSSHClient() {
        return new SSHClient();
    }

    protected SSHClient connect(String serverName) {
        SSHClient client = createSSHClient();
        Integer timeout = Config.<Integer> GetValue(ConfigValues.ConnectToServerTimeoutInSeconds) * 1000;
        client.setHardTimeout(timeout);
        client.setSoftTimeout(timeout);
        client.setHost(serverName, PORT);
        try {
            client.connect();
            return client;
        } catch (Exception e) {
            log.debug(String.format("Could not connect to server %1$s: %2$s", serverName, e.getMessage()));
            throw new RuntimeException(e);
        }
    }

    protected void authenticate(SSHClient client, String userId, String password) throws Exception {
        client.setUser(userId);
        client.setPassword(password);
        client.authenticate();
    }

    protected String executeCommand(SSHClient client) throws Exception {
        ByteArrayOutputStream out = new ConstraintByteArrayOutputStream(500);
        String command = Config.<String> GetValue(ConfigValues.GlusterPeerStatusCommand);
        client.executeCommand(command, null, out, null);
        return new String(out.toByteArray(), "UTF-8");
    }

    private String getFingerprint(String hostName) {
        SSHClient client = null;
        try {
            client = connect(hostName);
            return getFingerprint(client);
        } finally {
            if(client != null) {
                client.disconnect();
            }
        }
    }

    private String getFingerprint(SSHClient client) {
        PublicKey serverKey = client.getServerKey();
        if (serverKey == null) {
            log.error("Could not get server key");
            return null;
        }

        return OpenSSHUtils.getKeyFingerprintString(serverKey);
    }
}
