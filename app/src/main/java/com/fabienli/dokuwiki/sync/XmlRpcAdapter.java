package com.fabienli.dokuwiki.sync;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.fabienli.dokuwiki.R;
import com.fabienli.dokuwiki.tools.Logs;
import com.google.android.material.snackbar.Snackbar;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcSunHttpTransportFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

public class XmlRpcAdapter {
    protected String TAG = "XmlRpcAdapter";
    final XmlRpcClient client = new XmlRpcClient();
    protected Context _context;
    private String _user;
    private String _urlserver;
    private boolean _debug = false;
    protected XmlRpcClientConfigImpl _xmlConfig;
    protected Boolean _newApi;

    public XmlRpcAdapter(Context context){
        _context = context;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
        _user = settings.getString("user", "");
        _urlserver = settings.getString("serverurl", "");
        _debug = settings.getBoolean("debuglogs", false);
        _newApi = settings.getBoolean("newApi", false);
        Log.d(TAG,"New API setting from prefs: "+_newApi);
        Log.d(TAG,"Connecting to server <"+_urlserver + "> with user <"+_user+">");
        _xmlConfig = new XmlRpcClientConfigImpl();
        try {
            _xmlConfig.setServerURL(new URL(_urlserver));
            Boolean isBasicAuth = settings.getBoolean("basicauth", false);
            if(isBasicAuth) {
                String password = settings.getString("password", "");
                String user = settings.getString("user", "");;
                _xmlConfig.setBasicUserName(user);
                _xmlConfig.setBasicPassword(password);
            }
            //_xmlConfig.setUserAgent("Mozilla/5.0 (Android 9; Mobile; rv:67.0) Gecko/67.0 Firefox/67.0");
            client.setConfig(_xmlConfig);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    Object clientCallExecution(String methodName, Vector parameters) throws XmlRpcException {
        XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.instance();
        Object result = null;
        try {
            xmlRpcThrottler.waitIfNotInLimit();
            result = client.execute(methodName, parameters);
            xmlRpcThrottler.addCallNow();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public ArrayList<String> callMethod(String methodName, Vector<String> extraParams, String... params) {
        Vector parameters = getParametersVector(params);
        parameters.addElement(extraParams);
        return callMethod(methodName, parameters);
    }
    public ArrayList<String> callMethod(String methodName, String... params) {
        Vector parameters = getParametersVector(params);
        return callMethod(methodName, parameters);
    }
    public ArrayList<String> callMethod(String methodName, Vector parameters) {
        ArrayList<String> results = new ArrayList<String>();

        try {
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance(false, _debug));

            ensureLogin();

            Object result = "";
            try {
                Log.d(TAG,"Calling method: "+methodName);
                result = clientCallExecution(methodName, parameters);
                Log.d(TAG,"Called OK method: "+methodName);
            } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                Logs.getInstance().add("XmlRpc decode " + methodName+ " response error: " + exception);
                Log.e(TAG,"XmlRpc decode " + methodName+ " response error: " + exception);
                return null;
            }

            //Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().isArray())
            {
                //Log.d(TAG,"The result is a list: "+ result);
                Object[] aRestIt = ((Object[]) result);
                //Log.d(TAG,"The result list: "+ aRestIt);
                for(int i=0; i<aRestIt.length;i++){
                    //Log.d(TAG,"The result list#"+i+": "+ aRestIt[i]);
                    results.add(aRestIt[i].toString());
                }
                //Log.d(TAG,"The result size: "+ aRestIt.length);
            }
            else {
                //Log.d(TAG,"The result is: "+ result);
                results.add(result.toString());
            }


        } catch (Exception exception) {
            Log.e(TAG,"XmlRpc call " + methodName+ " error: " + exception);
            Logs.getInstance().add("XmlRpc call " + methodName+ " error: " + exception);
            return null;
        }
        return results;
    }

    public byte[] callMethodBinary(String methodName, String... params) {
        byte[] results = null;

        try {
            client.setTransportFactory(getXmlRpcSunHttpTransportFactoryInstance(true, false));

            Log.d(TAG,"2");

            Vector parameters = getParametersVector(params);

            ensureLogin();

            Log.d(TAG,"3 "+parameters.toString());
            Object result = "";
            try {
                result = clientCallExecution(methodName, parameters);
            } catch (org.apache.xmlrpc.client.XmlRpcClientException exception) {
                Logs.getInstance().add("XmlRpc decode " + methodName+ " response error: " + exception);
                Log.e(TAG,"XmlRpc decode " + methodName+ " response error: " + exception);
                result = exception.toString();
            }
            Log.d(TAG,"4");
            Log.d(TAG,"type:" + result.getClass());
            if(result.getClass().equals(byte[].class)) {
                Log.d(TAG,"The result is byte[]: "+ result);
                results = (byte[]) result;
            }


        } catch (Exception exception) {
            Log.e(TAG,"XmlRpc call " + methodName+ " error: " + exception);
            Logs.getInstance().add("XmlRpc call " + methodName+ " error: " + exception);
        }
        return results;
    }

    public Boolean useNewApi() {
        return _newApi;
    }
    public Boolean useOldApi() {
        return !_newApi;
    }

    protected void updateNewApiVersion() {
        try {
            // check API version
            Vector noParameter = new Vector();
            Object resultApiVersion = clientCallExecution("core.getAPIVersion", noParameter);
            Log.d(TAG, "The API version is: " + resultApiVersion);
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
            SharedPreferences.Editor editor = settings.edit();
            int apiVersion = Integer.parseInt((String) resultApiVersion.toString());
            if(apiVersion>10){
                Log.d(TAG, "Using new API version !");
                editor.putBoolean("newApi", true);
                editor.apply();
                _newApi = true;
            }
            else {
                Log.w(TAG, "Using old API version ... update your server!");
                editor.putBoolean("newApi", false);
                editor.apply();
                _newApi = false;

            }
        } catch (XmlRpcException e) {
            Log.d(TAG, "Error while getting API version: " + e);
        } catch (Exception exception) {
            Log.e(TAG,"API call version error: " + exception);
            Logs.getInstance().add("API call versionerror: " + exception);
        }

    }

    protected void ensureLogin() throws XmlRpcException {
        Log.d(TAG, "using the new API: "+_newApi);
        //Force reset login: CookiesHolder.Instance().cookies.clear();
        if(CookiesHolder.Instance().cookies.size() == 0) {
            //ensure we use the new API if available:
            updateNewApiVersion();

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(_context);
            String password = settings.getString("password", "");
            String user = settings.getString("user", "");

            // ensure correct throttling
            int throttlingLimit = Integer.parseInt(settings.getString("throttlingPerMin", "1000"));
            XmlRpcThrottler xmlRpcThrottler = XmlRpcThrottler.instance();
            xmlRpcThrottler.setLimit(throttlingLimit);

            Boolean loginResult;
            if(useOldApi()) {
                loginResult = loginDeprecatad(user, password);
            }
            else { // use New API
                // login
                Vector parametersLogin = new Vector();
                parametersLogin.addElement(user);
                parametersLogin.addElement(password);
                Object result = clientCallExecution("core.login", parametersLogin);
                Log.d(TAG,"The result login is: "+ result);
                Log.d(TAG,"The cookies size is: "+ CookiesHolder.Instance().cookies.size());
                loginResult = (Boolean) result;
            }

            if(! loginResult) {
                View toastView = ((AppCompatActivity) _context).findViewById(R.id.view_content);
                if (toastView == null) {
                    toastView = ((AppCompatActivity) _context).findViewById(R.id.webview);
                }
                Snackbar.make(toastView, "Login error ! please check user/password", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        }
        else if (! _newApi){
            Log.d(TAG,"No API version checked as already logged in, ensure we check");
            updateNewApiVersion();
        }
    }

    private Boolean loginDeprecatad(String user, String password) throws XmlRpcException {

        Vector parametersLogin = new Vector();
        parametersLogin.addElement(user);
        parametersLogin.addElement(password);
        Object result = clientCallExecution("dokuwiki.login", parametersLogin);
        Log.d(TAG,"The result login is: "+ result);
        Log.d(TAG,"The cookies size is: "+ CookiesHolder.Instance().cookies.size());

        return (Boolean) result;
    }

    protected Vector getParametersVector(String... params){
        Vector parameters = new Vector();
        for (int i=0; i<params.length; i++) {
            parameters.addElement(params[i]);
        }
        return parameters;
    }

    public XmlRpcSunHttpTransportFactory getXmlRpcSunHttpTransportFactoryInstance(Boolean isBinaryExpected, Boolean debug)
    {
        return new DwXmlRpcSunHttpTransportFactory(client, isBinaryExpected, debug);
    }
}
