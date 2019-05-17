package com.fabienli.dokuwiki.db;

import java.util.List;

public interface SyncActionListInterface {
    void handle(List<SyncAction> syncActions);
}
