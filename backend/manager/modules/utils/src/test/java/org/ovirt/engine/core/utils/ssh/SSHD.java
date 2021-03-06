package org.ovirt.engine.core.utils.ssh;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.keyprovider.AbstractKeyPairProvider;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.shell.ProcessShellFactory;

public class SSHD {

    class MyPasswordAuthenticator implements PasswordAuthenticator {
        String _user;
        String _password;

        public MyPasswordAuthenticator(String user, String password) {
            _user = user;
            _password = password;
        }

        @Override
        public boolean authenticate(String user, String password, ServerSession session) {
            return _user.equals(user) && _password.equals(password);
        }
    }

    class MyPublickeyAuthenticator implements PublickeyAuthenticator {
        String _user;
        PublicKey _key;

        public MyPublickeyAuthenticator(String user, PublicKey key) {
            _user = user;
            _key = key;
        }

        @Override
        public boolean authenticate(String user, PublicKey key, ServerSession session) {
            return user.equals(user) && _key.equals(key);
        }
    }

    class MyKeyPairProvider extends AbstractKeyPairProvider {
        KeyPair _keyPair;

        public MyKeyPairProvider(KeyPair keyPair) {
            _keyPair = keyPair;
        }

        @Override
        protected KeyPair[] loadKeys() {
            return new KeyPair[] { _keyPair };
        }
    }

    KeyPair _keyPair;
    String _user;
    String _userPassword;
    PublicKey _userKey;
    SshServer _sshd;

    public SSHD() {
        try {
            _keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        }
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        _sshd = SshServer.setUpDefaultServer();
        _sshd.setKeyPairProvider(new MyKeyPairProvider(_keyPair));
        _sshd.setShellFactory(
            new ProcessShellFactory(
                new String[] {
                    "/bin/sh",
                    "-i"
                }
            )
        );
        _sshd.setCommandFactory(
            new CommandFactory() {
                @Override
                public Command createCommand(String command) {
                    return new ProcessShellFactory(
                        new String[] {
                            "/bin/sh",
                            "-c",
                            command
                        }
                    ).create();
                }
            }
        );
    }

    public int getPort() {
        return _sshd.getPort();
    }

    public PublicKey getKey() {
        return _keyPair.getPublic();
    }

    public void setUser(String user, String password, PublicKey key) {
        _sshd.setPasswordAuthenticator(new MyPasswordAuthenticator(user, password));
        _sshd.setPublickeyAuthenticator(new MyPublickeyAuthenticator(user, key));
    }

    public void start() throws IOException {
        _sshd.start();
    }

    public void stop() {
        while (_sshd != null) {
            try {
                _sshd.stop(true);
                _sshd = null;
            }
            catch (InterruptedException e) {}
        }
    }


    public static void main(String [] args) throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();

        SSHD sshd = new SSHD();
        sshd.setUser("root", "password", keyPair.getPublic());
        sshd.start();
        System.out.println("Port: " + sshd.getPort());
    }
}
