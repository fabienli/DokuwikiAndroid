package com.fabienli.dokuwiki.usecase;

import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.db.SyncAction;
import com.fabienli.dokuwiki.usecase.callback.MediaRetrieveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MediaImport extends AsyncTask<String, Integer, String> {
    String TAG = "MediaRetrieve";
    protected AppDatabase _db;
    MediaRetrieveCallback _mediaRetrieveCallback = null;
    String _mediaPathName = "";
    protected String _mediaLocalDir = "";
    protected InputStream _inputStream = null;

    public MediaImport(AppDatabase db, String mediaLocalDir) {
        _db = db;
        _mediaLocalDir = mediaLocalDir;
    }

    public void importNewMedia(String newFileName, InputStream imageStream) {
        Media media = _db.mediaDao().findByName(newFileName);
        if(media != null){
            // new version
            //TODO
        }
        else {
            // new image to create
            media = new Media();
            media.id = newFileName;
            media.file = newFileName;
            media.isimg = "true";
            //TODO: other fields?
            _db.mediaDao().insertAll(media);
            SyncAction sa = new SyncAction();
            sa.verb = "PUT";
            sa.priority = "1";
            sa.name = newFileName;
            sa.rev = "";
            sa.data = _mediaLocalDir + "/" + newFileName;
            Log.d(TAG, "Will sync: "+sa.toText());
            _db.syncActionDao().deleteAll(sa);
            _db.syncActionDao().insertAll(sa);
        }
        saveNewMediaInCache(newFileName, imageStream);
    }

    public void saveNewMediaInCache(String newFileName, InputStream imageStream){
        Log.d(TAG, "Saving image: "+newFileName);
        File fileInCache = new File(_mediaLocalDir, newFileName);
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(fileInCache);

            byte[] buffer = new byte[4 * 1024]; // or other buffer size
            int read;

            while ((read = imageStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.d(TAG, "Saved");
    }

    public void importNewMediaAsync(String newFileName, InputStream imageStream, MediaRetrieveCallback mediaRetrieveCallback) {
        _inputStream = imageStream;
        _mediaRetrieveCallback = mediaRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newFileName);
    }

    @Override
    protected String doInBackground(String... params) {
        if(params.length==1) {
            importNewMedia(params[0], _inputStream);
            _mediaPathName = _mediaLocalDir + "/" + params[0];;
        }
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_mediaRetrieveCallback!=null)
            _mediaRetrieveCallback.mediaRetrieved(_mediaPathName);
    }
}
