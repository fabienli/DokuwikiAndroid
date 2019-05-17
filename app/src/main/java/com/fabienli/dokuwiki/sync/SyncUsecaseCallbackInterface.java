package com.fabienli.dokuwiki.sync;

import java.util.ArrayList;

public interface SyncUsecaseCallbackInterface {
    void processResultsList(ArrayList<String> iXmlrpcResults);
    void processResultsBinary(byte[] iXmlrpcResults);
}
