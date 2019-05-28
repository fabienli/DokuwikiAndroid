package com.fabienli.dokuwiki.usecase.callback;

import android.os.Build;

import androidx.annotation.RequiresApi;

public interface WikiSynchroCallback {
    void onceDone();
    default void progressUpdate(Integer... values){};
}
