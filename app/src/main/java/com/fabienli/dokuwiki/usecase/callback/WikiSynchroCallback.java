package com.fabienli.dokuwiki.usecase.callback;


public interface WikiSynchroCallback {
    void onceDone();
    default void progressUpdate(Integer... values){};
}
