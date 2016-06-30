package com.example.mittara.myapplication;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by mittara on 6/29/2016.
 */
public class ScanService extends Service{
    public final static String SERVICE_ACTION = "SERVICE_ACTION";
    public final static String SCAN_COMPLETE = "scancomplete";

    public final static String SCAN_RESULT = "scanresult";

    private boolean mIsRunning;
    private final IBinder mBinder = new ScanBinder();
    private static final String TAG = ScanService.class.getName();
    private NotificationManager mNotificationManager;

    private ResultBean mResultBean;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class ScanBinder extends Binder {
        ScanService getService() {
            return ScanService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mIsRunning) {
            mIsRunning = true;
            mNotificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();
        //for safety
        mNotificationManager.cancelAll();
    }

    private AsyncTask mScanTask;
    private boolean mContinueScaning;

    public void scanSdCard() {
        mContinueScaning = true;
        mScanTask = new ScanTask().execute();
    }

    public Bundle getScanResults() {
        if(mResultBean !=null){
            Bundle lBundle = new Bundle();
            lBundle.putSerializable(SCAN_RESULT,mResultBean);
            mResultBean = null;
            return lBundle;
        }
        return null;
    }
    public boolean isScaning() {
        return mContinueScaning;
    }

    public boolean isResultsAvailable() {
        if(mResultBean != null){
            return true;
        }
        return false;
    }


    public void stopTheScan() {
        mContinueScaning = false;
        mScanTask.cancel(true);
        removeNotification();
    }

    class ScanTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected void onPreExecute() {
            showScanNoitification();
        }

        @Override
        protected Void doInBackground(Void... params) {
            doTheScan();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            removeNotification();
            if(!isCancelled()) {
                notifyScanComplete();
            }
        }

    }

    private void doTheScan() {
        mResultBean = new ResultBean();

        LinkedHashMap<String,String> lTopFilesAndSizes = new LinkedHashMap<>();
        String lAvgFileSize = null;
        LinkedHashMap<String,String> lFrequentFileTypes = new LinkedHashMap<>();


        ContentResolver lContentResolver = getContentResolver();
        Uri lUri = MediaStore.Files.getContentUri("external");
        String[] lProjection = { MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.TITLE,
                MediaStore.Files.FileColumns.SIZE};
        String lOrderBy = MediaStore.Files.FileColumns.SIZE;
        Cursor lAllFilesCursor = lContentResolver.query(lUri, lProjection, null, null, lOrderBy+" DESC limit " + 10);

        int lTotalNoOfFiles = lAllFilesCursor.getCount();

        int lSizeIndex = lAllFilesCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE);
        int lNameIndex = lAllFilesCursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
        int lMimeIndex = lAllFilesCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);

        if (lAllFilesCursor.moveToFirst()){
            while(!lAllFilesCursor.isAfterLast()){
                String lName = lAllFilesCursor.getString(lNameIndex)+lAllFilesCursor.getString(lMimeIndex);
                long lSize = lAllFilesCursor.getLong(lSizeIndex);
                // do what ever you want here
                Log.d(TAG,"Name: "+lName+" size: "+getReadableFileSize(lSize));
                lTopFilesAndSizes.put(lName,getReadableFileSize(lSize));
                lAllFilesCursor.moveToNext();
            }
        }
        lAllFilesCursor.close();

        if(!mContinueScaning) return;
        //finiding average

        String[] lColumns = new String[] { "sum(" +  MediaStore.Files.FileColumns.SIZE + ")" };

        Cursor lAvgCursor = lContentResolver.query(lUri,lColumns,null,null,null);
        if(lAvgCursor.moveToFirst()) {
            long lTotalSizesSum = lAvgCursor.getLong(0);
            lAvgFileSize = getReadableFileSize(lTotalSizesSum/lTotalNoOfFiles);
        }
        lAvgCursor.close();
        Log.d(TAG,"AVGFileSize: "+lAvgFileSize);

        if(!mContinueScaning) return;

        //finding most frequent file types
        String[] lFrequentColumns = new String[] { MediaStore.Files.FileColumns.MIME_TYPE + ",count ("+MediaStore.Files.FileColumns.MIME_TYPE+") as cnt" };
        Cursor lFreqCursor =  lContentResolver.query(lUri,lFrequentColumns,"0 == 0) GROUP BY ("+MediaStore.Files.FileColumns.MIME_TYPE,null,"cnt Desc limit "+5);

        int lMimeTypeIndex = lFreqCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);
        int lFrquency = lFreqCursor.getColumnIndex("cnt");

        if(lFreqCursor.moveToFirst()){
            while(!lFreqCursor.isAfterLast()){
                String lMimeType =  lFreqCursor.getString(lMimeTypeIndex);
                String lCount =  lFreqCursor.getString(lFrquency);

                lFrequentFileTypes.put(lMimeType,lCount);
                Log.d(TAG,"lMimeType: "+lMimeType+" lCount: "+lCount);
                lFreqCursor.moveToNext();

            }
        }
        lFreqCursor.close();

        mResultBean.setmAvgFileSize(lAvgFileSize);
        mResultBean.setmFrequentFileTypes(lFrequentFileTypes);
        mResultBean.setmTopFilesAndSizes(lTopFilesAndSizes);

    }

    public String getReadableFileSize(long pSize) {
        if(pSize <= 0) return "0";
        final String[] lUnits = new String[] { "B", "kB", "MB", "GB", "TB" };
        int lDigitGroups = (int) (Math.log10(pSize)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(pSize/Math.pow(1024, lDigitGroups)) + " " + lUnits[lDigitGroups];
    }

    private final int NOTIFICATION_ID =1;
    private void showScanNoitification() {

        Intent lIntent = new Intent(this, MainActivity.class);
        PendingIntent lPendingIntent = PendingIntent.getActivity(this, 0, lIntent, 0);

        Notification lNotification  = new Notification.Builder(this)
                .setContentTitle("Scaning")
                .setContentText("Scaning Files")
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(lPendingIntent).build();

        mNotificationManager.notify(NOTIFICATION_ID, lNotification);
    }

    private void removeNotification() {
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

    private void notifyScanComplete() {
        Intent lIntent = new Intent();
        lIntent.setAction(SERVICE_ACTION);
        lIntent.putExtra(SCAN_COMPLETE,true);
        sendBroadcast(lIntent);
    }

}
