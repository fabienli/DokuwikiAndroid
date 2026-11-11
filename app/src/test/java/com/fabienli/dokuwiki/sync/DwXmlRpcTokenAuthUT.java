package com.fabienli.dokuwiki.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcClientRequestImpl;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class DwXmlRpcTokenAuthUT {

    private Map<String, String> sentHeaders(String apiToken, boolean basicAuth) throws XmlRpcClientException {
        final Map<String, String> headers = new HashMap<>();
        XmlRpcClient client = new XmlRpcClient();
        XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        if (basicAuth) {
            config.setBasicUserName("user");
            config.setBasicPassword("pwd");
        }
        client.setConfig(config);
        DwXmlRpcSunHttpTransport transport = new DwXmlRpcSunHttpTransport(client, false, false, apiToken) {
            @Override
            protected void setRequestHeader(String pHeader, String pValue) {
                headers.put(pHeader, pValue);
            }
        };
        transport.initHttpHeaders(new XmlRpcClientRequestImpl(config, "core.getAPIVersion", new Object[0]));
        return headers;
    }

    @Test
    public void tokenIsSentAsBearerAuthorization() throws XmlRpcClientException {
        Map<String, String> headers = sentHeaders("my.jwt.token", false);
        assertEquals("Bearer my.jwt.token", headers.get("Authorization"));
    }

    @Test
    public void tokenOverridesBasicAuthorization() throws XmlRpcClientException {
        Map<String, String> headers = sentHeaders("my.jwt.token", true);
        assertEquals("Bearer my.jwt.token", headers.get("Authorization"));
    }

    @Test
    public void noAuthorizationHeaderWithoutToken() throws XmlRpcClientException {
        assertFalse(sentHeaders("", false).containsKey("Authorization"));
    }
}
