package com.fabienli.dokuwiki.sync;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class TestXmlRpc {

    /**
     * Test with empty array value returned
     */
    @Test
    public void test_spacesInAsnwer(){
        final XmlRpcClient client = new XmlRpcClient();
        DwXmlRpcSunHttpTransport dwXmlRpcSunHttpTransport = new DwXmlRpcSunHttpTransport(client, false, false);
        String inputData = "   <?xml version=\"1.0\"?>\n" +
                "<methodResponse><params>\n" +
                "    <param>\n" +
                "      <value>\n" +
                "        <string>test value</string></value>\n" +
                "    </param>\n" +
                "  </params></methodResponse>";
        InputStream pStream = new ByteArrayInputStream(inputData.getBytes(StandardCharsets.UTF_8));
        XmlRpcClientConfigImpl xmlRpcClientConfig = new XmlRpcClientConfigImpl();
        client.setConfig(xmlRpcClientConfig);
        try {
            dwXmlRpcSunHttpTransport.readResponse(xmlRpcClientConfig, pStream);
        } catch (XmlRpcException e) {
            System.out.println(inputData);
            e.printStackTrace();
            assert(false);
        }
        // reaching here means success for this test
    }
}
