package com.fabienli.dokuwiki.sync;

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Vector;

public class XmlRpcAdapterFile extends XmlRpcAdapter{

    public XmlRpcAdapterFile(Context context) {
        super(context);
    }

    public XmlRpcAdapterFile(XmlRpcAdapter parent){
        super(parent._context);
    }

    @Override
    protected Vector getParametersVector(String... params){
        Vector parameters = new Vector();
        for (int i=0; i<params.length; i++) {
            if(params[i].startsWith("file://")){
                File file = new File(params[i].substring(7));
                byte[] b = new byte[(int)file.length()];
                try {
                    FileInputStream fileInputStream = new FileInputStream(file);
                    fileInputStream.read(b);
                    parameters.addElement(b);
                    fileInputStream.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else
            {
                parameters.addElement(params[i]);
            }
        }
        return parameters;
    }
}
