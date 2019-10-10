package com.fabienli.dokuwiki.usecase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

import com.fabienli.dokuwiki.WikiCacheUiOrchestrator;
import com.fabienli.dokuwiki.db.AppDatabase;
import com.fabienli.dokuwiki.db.Media;
import com.fabienli.dokuwiki.sync.MediaDownloader;
import com.fabienli.dokuwiki.sync.MediaInfoRetriever;
import com.fabienli.dokuwiki.sync.XmlRpcAdapter;
import com.fabienli.dokuwiki.usecase.callback.MediaRetrieveCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MediaRetrieve extends AsyncTask<String, Integer, String> {
    String TAG = "MediaRetrieve";
    protected AppDatabase _db;
    MediaRetrieveCallback _mediaRetrieveCallback = null;
    String _mediaPathName = "";
    XmlRpcAdapter _xmlRpcAdapter;
    protected String _mediaLocalDir = "";
    protected Boolean _mediaDownloaded = false;
    protected Boolean _mediaResized = false;

    public MediaRetrieve(AppDatabase db, XmlRpcAdapter xmlRpcAdapter, String mediaLocalDir) {
        _db = db;
        _xmlRpcAdapter = xmlRpcAdapter;
        _mediaLocalDir = mediaLocalDir;
    }

    public String getMedia(String mediaId, String mediaRelativePathname, int targetW, int targetH, boolean forceDownload) {
        Log.d(TAG, "downloading file "+mediaId+" to: "+_mediaLocalDir+"/"+mediaRelativePathname);
        // check if media is in DB
        Media media = _db.mediaDao().findByName(mediaId);
        if(media == null) {
            MediaInfoRetriever mediaInfoRetriever = new MediaInfoRetriever(_xmlRpcAdapter);
            media = mediaInfoRetriever.retrieveMediaInfo(mediaId);
            media.id = mediaId;
            media.file = mediaRelativePathname;
            _db.mediaDao().insertAll(media);
        }

        // check if media is in local folder
        String newlocalFilename = UrlConverter.getLocalFileName(mediaRelativePathname, targetW, targetH);
        File originalfile = new File(_mediaLocalDir, mediaRelativePathname);
        File file = new File(_mediaLocalDir, newlocalFilename);
        if(file.exists() && !forceDownload) { // File is there, in correct size
            Log.d(TAG, "Local file already there !");
            return newlocalFilename;
        }

        // 1. retrieve file from server if not there
        if(!originalfile.exists() || forceDownload) {
            MediaDownloader mediaDownloader = new MediaDownloader(_xmlRpcAdapter);
            byte[] fcontent = mediaDownloader.retrieveMedia(mediaId);

            // 2. save file to our local disk
            if (fcontent == null)
                return "";
            File parent = originalfile.getParentFile();
            if (!parent.exists())
                parent.mkdirs();
            try {
                Log.d(TAG, "saving file to: "+originalfile.getAbsolutePath());
                FileOutputStream fw = new FileOutputStream(originalfile.getAbsoluteFile());
                fw.write(fcontent);
                fw.close();
                _mediaDownloaded = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 3. ensure the corect size is also there
        if(targetW>0 || targetH > 0)
            return createLocalFileResized(mediaRelativePathname, targetW, targetH);

        return mediaRelativePathname;
    }

    public String createLocalFileResized(String localPath, int targetW, int targetH){
        String newlocalFilename = UrlConverter.getLocalFileName(localPath, targetW, targetH);
        Log.d(TAG, "Resizing "+localPath+" to "+newlocalFilename);

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(_mediaLocalDir+"/"+localPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = 1;
        if ((targetW > 0) && (targetH > 0)) {
            scaleFactor = Math.min(photoW/targetW, photoH/targetH);
        }
        else if(targetW > 0) {
            scaleFactor = photoW/targetW;
        }
        else if(targetH > 0) {
            scaleFactor = photoH/targetH;
        }

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true; //Deprecated API 21

        Bitmap newBitmap = BitmapFactory.decodeFile(_mediaLocalDir+"/"+localPath, bmOptions);
        if(newBitmap == null) // invalid source file
        {
            Log.e(TAG, "Invalid file ! " + localPath);
            File invalidFile = new File(_mediaLocalDir, localPath);
            invalidFile.delete();
            return "";
        }

        if (targetW>0 && targetH>0) {
            Log.d(TAG, "targetW:"+targetW+" targetH:"+targetH+" to w:"+newBitmap.getWidth()+ " h:"+newBitmap.getHeight());
            // should be cropped to ensure width and height
            int startX = (newBitmap.getWidth() - targetW) / 2;
            int startY = (newBitmap.getHeight() - targetH) / 2;
            newBitmap = Bitmap.createBitmap(newBitmap, startX, startY , targetW , targetH);
        }
        try {
            File file = new File(_mediaLocalDir, newlocalFilename);
            file.createNewFile();
            FileOutputStream ostream = new FileOutputStream(file);
            newBitmap.compress(Bitmap.CompressFormat.PNG, 100, ostream);
            ostream.close();
            _mediaResized = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return newlocalFilename;
    }

    public void getMediaAsync(String mediaId, String mediaLocalPath, int targetW, int targetH, MediaRetrieveCallback mediaRetrieveCallback) {
        _mediaRetrieveCallback = mediaRetrieveCallback;
        executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mediaId, mediaLocalPath, ""+targetW, ""+targetH);
    }


    @Override
    protected String doInBackground(String... params) {
        if(params.length == 4)
            _mediaPathName = getMedia(params[0], params[1], Integer.parseInt(params[2]), Integer.parseInt(params[3]), false);
        return "ok";
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if(_mediaRetrieveCallback!=null)
            if(_mediaDownloaded || _mediaResized)
                _mediaRetrieveCallback.mediaRetrieved(_mediaPathName);
            else
                _mediaRetrieveCallback.mediaWasAlreadyThere(_mediaPathName);
    }
}
