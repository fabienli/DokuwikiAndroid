package com.fabienli.dokuwiki.usecase.callback;

public interface MediaRetrieveCallback {
    void mediaRetrieved(String mediaPathName);
    default void mediaWasAlreadyThere(String mediaPathName){};
}
