package com.example.mittara.myapplication;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private ScanService mScanServiceBinder;
    private ResultReceiver mResultReceiver;
    private Button mScanButton;
    private View mScaningText;
    private LinearLayout mResultsLayout;
    private View mShareResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startScanService();
        init();
    }

    private void init() {
        mScanButton = (Button)findViewById(R.id.scan_button);
        mScaningText = findViewById(R.id.scaning_text);
        mResultsLayout = (LinearLayout)findViewById(R.id.results_layout);
        mShareResults = findViewById(R.id.share_results);

        mScanButton.setOnClickListener(this);
        mShareResults.setOnClickListener(this);
        mScanButton.setTag(false);

        mResultReceiver = new ResultReceiver();
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.scan_button:
                    scanForTheData();
                break;
            case R.id.share_results:
                shareResults();
                break;
        }
    }

    private void scanForTheData() {

        if((boolean)mScanButton.getTag()) {
            //stop the scan
            if(mScanServiceBinder != null && mScanServiceBinder.isScaning()){
                mScanServiceBinder.stopTheScan();
                mScanButton.setText("SCAN");
                mScaningText.setVisibility(View.GONE);
            }

        }else{
            //start the scan
            if(CheckStoragePermission()) {
                if(mScanServiceBinder != null){
                    mScanServiceBinder.scanSdCard();
                    mScanButton.setText("STOP SCANING");
                    mScaningText.setVisibility(View.VISIBLE);
                }
            }
        }

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        stopScanService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindScanService();
        registerReceiver(mResultReceiver,new IntentFilter(ScanService.SERVICE_ACTION));
        if(mScanServiceBinder != null && mScanServiceBinder.isResultsAvailable()){
            displayResults();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindScanService();
        unregisterReceiver(mResultReceiver);
    }

    private void displayResults() {
        Bundle lResultsBundle = mScanServiceBinder.getScanResults();
        mScanButton.setText("SCAN");
        mScaningText.setVisibility(View.GONE);
        mShareResults.setVisibility(View.VISIBLE);
        if(lResultsBundle != null){
            ResultBean lResultBean = (ResultBean) lResultsBundle.getSerializable(ScanService.SCAN_RESULT);

            mResultsLayout.removeAllViews();

            TextView lTextView = new TextView(this);
            LinearLayout.LayoutParams lLayoutarams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lLayoutarams.setMargins(0,(int)getPixelValue(10),0,0);
            lTextView.setLayoutParams(lLayoutarams);
            lTextView.setText(" Top Files and Their File Sizes");
            lTextView.setTypeface(null, Typeface.BOLD);
            mResultsLayout.addView(lTextView);


            LinkedHashMap<String, String> lTopFilesAndSizes =  lResultBean.getmTopFilesAndSizes();

            if(lTopFilesAndSizes != null) {
                for (Map.Entry<String, String> entry : lTopFilesAndSizes.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    TextView lTextView1 = new TextView(this);
                    lTextView1.setLayoutParams(lLayoutarams);
                    lTextView1.setText(key + "  " + value);
                    mResultsLayout.addView(lTextView1);

                }
            }


            TextView lTextView2 = new TextView(this);

            lTextView2.setLayoutParams(lLayoutarams);
            lTextView2.setText("AVG File Size: "+lResultBean.getmAvgFileSize());
            lTextView2.setTypeface(null, Typeface.BOLD);
            mResultsLayout.addView(lTextView2);

            TextView lTextView3 = new TextView(this);
            lTextView3.setLayoutParams(lLayoutarams);
            lTextView3.setText("Most Frequent files and Their Frequencies");
            lTextView3.setTypeface(null, Typeface.BOLD);
            mResultsLayout.addView(lTextView3);

            LinkedHashMap<String, String> lFrequentFiles =  lResultBean.getmFrequentFileTypes();
            if(lFrequentFiles != null){
                for (Map.Entry<String, String> entry : lFrequentFiles.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    TextView lTextView1 = new TextView(this);
                    lTextView1.setLayoutParams(lLayoutarams);
                    lTextView1.setText(key + "  " + value);
                    mResultsLayout.addView(lTextView1);

                }
            }

        }

    }


    public void startScanService() {
        startService(new Intent(getBaseContext(), ScanService.class));
    }

    public void stopScanService() {
        stopService(new Intent(getBaseContext(), ScanService.class));
    }

    private void bindScanService(){
        Intent intent= new Intent(this, ScanService.class);
        bindService(intent, mServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    private void unbindScanService() {
        if(mScanServiceBinder != null)
            unbindService(mServiceConnection);
    }

    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder pBinder) {
            mScanServiceBinder = ((ScanService.ScanBinder)pBinder).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mScanServiceBinder = null;
        }
    };

    private final int REQUEST_CODE_ASK_PERMISSIONS = 1;
    @TargetApi(Build.VERSION_CODES.M)
    public boolean CheckStoragePermission() {
        int lReadPermissionCheckRead = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        int lWritePermissionCheckRead = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (lReadPermissionCheckRead != PackageManager.PERMISSION_GRANTED && lWritePermissionCheckRead != PackageManager.PERMISSION_GRANTED ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale( this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);
            } else {

                ActivityCompat.requestPermissions( this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        REQUEST_CODE_ASK_PERMISSIONS);


            }
            return false;
        } else
            return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        bindScanService();
                } else {
                    // Permission Denied
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT)
                            .show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }



    private class ResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if(arg1 != null && arg1.getBooleanExtra(ScanService.SCAN_COMPLETE,false)) {
                mHandler.sendEmptyMessage(UPDATE_UI_WITH_RESULTS);
            }


        }
    }

    private final int UPDATE_UI_WITH_RESULTS = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_UI_WITH_RESULTS:
                    if(mScanServiceBinder != null && mScanServiceBinder.isResultsAvailable()){
                        displayResults();
                    }
                    break;
            }

            super.handleMessage(msg);

        }
    };

    protected float getPixelValue(float base) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, base, getResources().getDisplayMetrics());
    }

    private void shareResults() {
        Bitmap lBitmap = Bitmap.createBitmap(mResultsLayout.getWidth(), mResultsLayout.getHeight(),Bitmap.Config.ARGB_8888);
        Canvas lCanvas = new Canvas(lBitmap);
        Drawable lDrawable =mResultsLayout.getBackground();
        if (lDrawable!=null)
            lDrawable.draw(lCanvas);
        else
            lCanvas.drawColor(Color.WHITE);
        mResultsLayout.draw(lCanvas);


        String root = Environment.getExternalStorageDirectory().toString();
        File myDir = new File(root + "/scan_results");
        myDir.mkdirs();
        Random generator = new Random();
        String fname = "temp.png";
        File file = new File (myDir, fname);
        if (file.exists ()) file.delete ();
        try {
            FileOutputStream out = new FileOutputStream(file);
            lBitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.flush();
            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

            Intent shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(myDir, "temp.png")));
            shareIntent.setType("image/PNG");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "send"));
    }

}
