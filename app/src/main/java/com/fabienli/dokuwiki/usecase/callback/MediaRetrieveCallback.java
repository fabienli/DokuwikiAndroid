package com.fabienli.dokuwiki.usecase.callback;

public interface MediaRetrieveCallback {
    void mediaRetrieved(String mediaPathName);
    void mediaWasAlreadyThere(String mediaPathName);
}
