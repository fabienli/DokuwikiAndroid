package com.fabienli.dokuwiki.sync;

import android.util.Log;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcRequest;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientException;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransport;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;
import org.apache.xmlrpc.client.XmlRpcTransport;
import org.apache.xmlrpc.common.XmlRpcStreamRequestConfig;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DwXmlRpcSunHttpTransportFactory extends XmlRpcSunHttpTransportFactory {

    private Boolean isBinaryExpected;
    private XmlRpcClient client;
    public DwXmlRpcSunHttpTransportFactory(XmlRpcClient client, Boolean isBinaryExpected) {
        super(client);
        this.client = client;
        this.isBinaryExpected = isBinaryExpected;
    }

    public XmlRpcTransport getTransport() {
        return new DwXmlRpcSunHttpTransport(client, isBinaryExpected);
    }
}

class DwXmlRpcSunHttpTransport extends XmlRpcSunHttpTransport {
    private Boolean isBinaryExpected;
    private URLConnection conn;
    private String TAG = "DwXmlRpcSunHttpTransport";


    public DwXmlRpcSunHttpTransport(XmlRpcClient pClient, Boolean isBinaryExpected) {
        super(pClient);
        this.isBinaryExpected = isBinaryExpected;
    }
    @Override
    protected URLConnection newURLConnection(URL pURL) throws IOException {
        conn = super.newURLConnection(pURL);
        return conn;
    }

    @Override
    protected void initHttpHeaders(XmlRpcRequest request) throws XmlRpcClientException {
        super.initHttpHeaders(request);
        Log.d(TAG, "initHttpHeaders with cookies size: " + CookiesHolder.Instance().cookies.size());
        if (CookiesHolder.Instance().cookies.size() > 0) {
            StringBuilder commaSep = new StringBuilder();
            for (String str : CookiesHolder.Instance().cookies) {
                commaSep.append(str);
                commaSep.append(";");
            }
            setRequestHeader("Cookie", commaSep.toString());
            //expected cookie format: DokuWiki=1afac8db031462045151b6eb372baeb0;DW7fa065a06cb74c216c145cfbc46ac7d3=ZmFibWVu%7C0%7Cj0DduBEkSf1FLprB3MwmFj75UqlLUx20Nonb4vCgCRI%3D;
            Log.d(TAG, "Cookies: " + commaSep.toString());
        }
    }

    @Override
    protected void close() throws XmlRpcClientException {
        getCookies(conn);
    }

    @Override
    protected Object readResponse(XmlRpcStreamRequestConfig pConfig, InputStream pStream) throws XmlRpcException {
        // if Binary is expected, don't try to alter it
        if(isBinaryExpected)
            return super.readResponse(pConfig, pStream);

        // not Binary, so we can adapt a few things:
        final StringBuffer sb = new StringBuffer();
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(pStream));
            String line = reader.readLine();
            while (line != null) {
                //It seems that the date format is not fully handled with ISO 8601, work this around by updating all dates
                //<dateTime.iso8601>2018-10-16T12:08:21+0000</dateTime.iso8601>
                // to
                //<dateTime.iso8601>20181016T12:08:21+0000</dateTime.iso8601>
                line = line.replaceAll("<dateTime.iso8601>(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)", "<dateTime.iso8601>$1$2$3");
                sb.append(line);
                line = reader.readLine();
            }
        } catch (final IOException e) {
            Log.d(TAG, "While reading server response" + e.toString());
        }
        Log.d(TAG, sb.toString());

        final ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes());
        return super.readResponse(pConfig, bais);
    }

    private void getCookies(URLConnection conn) {
        if (CookiesHolder.Instance().cookies.size() == 0) {
            Map<String, List<String>> headers = conn.getHeaderFields();
            if (// avoid NPE
                    headers.containsKey(// avoid NPE
                            "Set-Cookie")) {
                List<String> vals = headers.get("Set-Cookie");
                for (String str : vals) {
                    Log.d(TAG, "Cookie origin: " + str);
                    String[] cookieslice = str.split(";");
                    if (cookieslice.length > 0) {
                        String cookiemain = cookieslice[0];
                        if (cookiemain.startsWith("DokuWiki")) {
                            CookiesHolder.Instance().cookies.add(cookiemain);
                            Log.d(TAG, "Cookie used: " + cookiemain);
                        } else if (cookiemain.endsWith("deleted")) {
                            Log.d(TAG, "Cookie ignored");
                        } else if (cookiemain.startsWith("DW")) {
                            CookiesHolder.Instance().cookies.add(cookiemain);
                            Log.d(TAG, "Cookie used: " + cookiemain);
                        } else {
                            Log.d(TAG, "Cookie ignored");
                        }
                    }
                }
            }
        }
    }
}