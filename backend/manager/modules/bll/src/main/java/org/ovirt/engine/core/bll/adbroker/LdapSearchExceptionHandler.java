package org.ovirt.engine.core.bll.adbroker;

import java.net.ConnectException;

import javax.naming.OperationNotSupportedException;
import javax.security.sasl.SaslException;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.ovirt.engine.core.utils.log.Log;
import org.ovirt.engine.core.utils.log.LogFactory;
import org.ovirt.engine.core.utils.kerberos.AuthenticationResult;
import org.springframework.ldap.AuthenticationException;
import org.springframework.ldap.CommunicationException;

public class LdapSearchExceptionHandler implements ExceptionHandler<LdapSearchExceptionHandlingResponse, LdapCredentials> {

    private static final Log log = LogFactory.getLog(LdapSearchExceptionHandler.class);

    @Override
    public LdapSearchExceptionHandlingResponse handle(Exception e, LdapCredentials params) {
        LdapSearchExceptionHandlingResponse response = new LdapSearchExceptionHandlingResponse();
        if (e instanceof AuthenticationResultException) {
            handleEngineDirectoryServiceException(response, e);
        } else if (e instanceof AuthenticationException) {
            handleAuthenticationException(response);
        } else if (e instanceof CommunicationException) {
            handleCommunicationException(response, e);
        } else if (e instanceof InterruptedException) {
            handleInterruptException(response, e);
        } else {
            boolean found = false;
            for (Throwable throwable : ExceptionUtils.getThrowables(e)) {
                if ((throwable instanceof SaslException || throwable instanceof ConnectException)) {
                    handleSaslException(response, throwable);
                    found = true;
                    break;
                } else if (throwable instanceof OperationNotSupportedException) {
                    handleOperationException(response, throwable, params);
                    found = true;
                    break;
                }
            }
            if (!found) {
                handleGeneralException(response, e);
            }
        }
        return response;
    }

    private void handleGeneralException(LdapSearchExceptionHandlingResponse response, Exception e) {
        response.setTranslatedException(e)
                .setTryNextServer(true)
                .setServerScore(Score.LOW);
    }

    private void handleSaslException(LdapSearchExceptionHandlingResponse response, Throwable cause) {
        response.setServerScore(Score.LOW)
                .setTranslatedException(new AuthenticationResultException(AuthenticationResult.CONNECTION_ERROR,
                        "General connection problem due to " + cause))
                .setTryNextServer(true);
    }

    private void handleOperationException(LdapSearchExceptionHandlingResponse response,
            Throwable throwable, LdapCredentials credentials) {
        response.setServerScore(Score.HIGH)
                .setTranslatedException(new AuthenticationResultException(AuthenticationResult.USER_ACCOUNT_DISABLED_OR_LOCKED,
                        throwable))
                .setTryNextServer(false);
        //Account may get locked between kerberos authentication and ldap querying.
        //The audit log infrastructure prevents double logging in case the scenario in the above line does not occur (which is in most cases)
        LdapBrokerUtils.logEventForUser(credentials.getUserName(), AuthenticationResult.USER_ACCOUNT_DISABLED_OR_LOCKED.getAuditLogType());
    }

    private void handleInterruptException(LdapSearchExceptionHandlingResponse response, Throwable cause) {
        response.setServerScore(Score.HIGH)
                .setTranslatedException((Exception) cause)
                .setTryNextServer(false);
    }

    private void handleCommunicationException(LdapSearchExceptionHandlingResponse response, Throwable cause) {
        log.error("Error in communicating with LDAP server " + cause.getMessage());
        response.setServerScore(Score.LOW).setTryNextServer(true).setTranslatedException((Exception) cause);
    }

    private void handleAuthenticationException(LdapSearchExceptionHandlingResponse response) {
        log.error("Ldap authentication failed. Please check that the login name , password and path are correct. ");
        AuthenticationResultException ex =
                new AuthenticationResultException(AuthenticationResult.OTHER);
        response.setServerScore(Score.HIGH)
                .setTranslatedException(ex)
                .setTryNextServer(false);
    }

    private void handleEngineDirectoryServiceException(LdapSearchExceptionHandlingResponse response, Throwable cause) {
        response.setTranslatedException((AuthenticationResultException) cause);
        switch (((AuthenticationResultException) cause).getResult()) {
        // connection error or timeout indicates problems with the sever so handling the same.
        case CONNECTION_ERROR:
        case CONNECTION_TIMED_OUT:
        case CLOCK_SKEW_TOO_GREAT:
            response.setServerScore(Score.LOW).setTryNextServer(true);
            break;
        default:
            response.setServerScore(Score.HIGH).setTryNextServer(false);
            break;
        }
    }

    private void handleTimeout(LdapSearchExceptionHandlingResponse response) {
        response.setTryNextServer(true)
                .setTranslatedException(
                        new AuthenticationResultException(AuthenticationResult.CONNECTION_TIMED_OUT,
                                "Connection to to server has timed out."))
                .setServerScore(Score.LOW);
    }

}
