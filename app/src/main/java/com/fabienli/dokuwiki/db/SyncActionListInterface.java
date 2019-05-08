package com.fabienli.dokuwiki.db;

import java.util.List;

public interface SyncActionListInterface {
    public void handle(List<SyncAction> syncActions);
}
