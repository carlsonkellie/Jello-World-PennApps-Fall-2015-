package com.example.lily.exercise;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.*;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{
    private int foodCal, burnedCal, avgBurnedCal;        //calories of food in the locker, calories burned
    private boolean lockStatus; //status of the locker: true if currently locked
    private DataSet dataSet;    //dataSet of calories
    private List<Integer> dataList;
    private static final int REQUEST_OAUTH = 1;

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;

    private GoogleApiClient mClient = null;
    private Button lockBtn, unlockBtn, backBtn, nextBtn1, nextBtn2;
    private TextView text1, text4, text5, text6;
    private EditText text2, text3;
    public static final String TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lockStatus = false;
        text1 = (TextView) findViewById(R.id.text1);
        text2 = (EditText) findViewById(R.id.text2);
        text3 = (EditText) findViewById(R.id.text3);
        text4 = (TextView) findViewById(R.id.text4);
        text5 = (TextView) findViewById(R.id.text5);
        text6 = (TextView) findViewById(R.id.text6);

        lockBtn = (Button) findViewById(R.id.lockBtn);
        nextBtn1 = (Button) findViewById(R.id.nextBtn1);
        unlockBtn = (Button) findViewById(R.id.unlockBtn);
        nextBtn2 = (Button) findViewById(R.id.nextBtn2);
        backBtn = (Button) findViewById(R.id.backBtn);
        unlockBtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                requestData();
                dumpDataSet(dataSet);
                float tot = 0;
                for (int val: dataList){
                    tot+=val;
                }
                tot/=dataList.size();
                if (Math.abs(tot-1800)>80){
                    text6.setVisibility(View.VISIBLE);
                    backBtn.setVisibility(View.VISIBLE);
                }
                else {
                    Log.i(TAG, "unlocked!");
                    lockBtn.setVisibility(View.GONE);
                    text1.setVisibility(View.INVISIBLE);
                    text3.setVisibility(View.VISIBLE);
                    nextBtn1.setVisibility(View.VISIBLE);
                }
            }
        });
        lockBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Log.i(TAG, "Locked!");
                unlockBtn.setVisibility(View.GONE);
                text1.setVisibility(View.INVISIBLE);
                text2.setVisibility(View.VISIBLE);
                nextBtn1.setVisibility(View.VISIBLE);
            }
        });
        nextBtn1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                Log.i(TAG, "unlocked!");
                lockBtn.setVisibility(View.GONE);
                text3.setVisibility(View.INVISIBLE);
                text2.setVisibility(View.INVISIBLE);
                nextBtn1.setVisibility(View.GONE);
                text4.setVisibility(View.VISIBLE);
                backBtn.setVisibility(View.VISIBLE);
            }
        });
        nextBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "unlocked!");
                lockBtn.setVisibility(View.GONE);
                text3.setVisibility(View.INVISIBLE);
                text2.setVisibility(View.INVISIBLE);
                text5.setVisibility(View.VISIBLE);
                nextBtn2.setVisibility(View.GONE);
                backBtn.setVisibility(View.VISIBLE);
            }
        });
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "unlocked!");
                lockBtn.setVisibility(View.VISIBLE);
                unlockBtn.setVisibility(View.VISIBLE);
                text1.setVisibility(View.VISIBLE);
                text3.setVisibility(View.INVISIBLE);
                text2.setVisibility(View.INVISIBLE);
                text5.setVisibility(View.INVISIBLE);
                nextBtn2.setVisibility(View.GONE);
                backBtn.setVisibility(View.GONE);
            }
        });

        if (savedInstanceState != null) {
            authInProgress = savedInstanceState.getBoolean(AUTH_PENDING);
        }

        buildFitnessClient();
    }

    /**
     *  Build a {@link GoogleApiClient} that will authenticate the user and allow the application
     *  to connect to Fitness APIs. The scopes included should match the scopes your app needs
     *  (see documentation for details). Authentication will occasionally fail intentionally,
     *  and in those cases, there will be a known resolution, which the OnConnectionFailedListener()
     *  can address. Examples of this include the user never having signed in before, or having
     *  multiple accounts on the device and needing to specify which account to use, etc.
     */
    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(this)
                .addApi(Fitness.SENSORS_API)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {

                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");
                                // Now you can make calls to the Fitness APIs.
                                // Put application specific code here.
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        new GoogleApiClient.OnConnectionFailedListener() {
                            // Called whenever the API client fails to connect.
                            @Override
                            public void onConnectionFailed(ConnectionResult result) {
                                Log.i(TAG, "Connection failed. Cause: " + result.toString());
                                if (!result.hasResolution()) {
                                    // Show the localized error dialog
                                    GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(),
                                            MainActivity.this, 0).show();
                                    return;
                                }
                                // The failure has a resolution. Resolve it.
                                // Called typically when the app is not yet authorized, and an
                                // authorization dialog is displayed to the user.
                                if (!authInProgress) {
                                    try {
                                        Log.i(TAG, "Attempting to resolve failed connection");
                                        authInProgress = true;
                                        result.startResolutionForResult(MainActivity.this,
                                                REQUEST_OAUTH);
                                    } catch (IntentSender.SendIntentException e) {
                                        Log.e(TAG,
                                                "Exception while starting resolution activity", e);
                                    }
                                }
                            }
                        }
                )
                .build();
    }
    @Override
    protected void onStart() {
        super.onStart();
        // Connect to the Fitness API
        Log.i(TAG, "Connecting...");
        mClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mClient.isConnected()) {
            mClient.disconnect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(AUTH_PENDING, authInProgress);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    /*
    public void buttonOnClick(View v){
        Button button = (Button) v;
    }*/

    public void requestData() {
        Calendar cal = Calendar.getInstance();
        Date now = new Date();
        cal.setTime(now);
        long endTime = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_WEEK, -1);
        long startTime = cal.getTimeInMillis();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        Log.i("Start", "Range Start: " + dateFormat.format(startTime));
        Log.i("End", "Range End: " + dateFormat.format(endTime));

        DataReadRequest readRequest = new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few steps and a timestamp.
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                //.aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                        // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                        // bucketByTime allows for a time span, whereas bucketBySession would allow
                        // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        /*//http://stackoverflow.com/questions/28234525/fetching-google-fit-data-into-android-application
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_STEP_COUNT_DELTA)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();*/
        // Invoke the History API to fetch the data with the query and await the result of
        // the read request.

        // Create a data source
        DataSource dataSource = new DataSource.Builder()
                .setAppPackageName(this)
                .setDataType(DataType.TYPE_CALORIES_EXPENDED)
                .setName(TAG + " - step count")
                .setType(DataSource.TYPE_RAW)
                .build();

// Create a data set
        dataSet = DataSet.create(dataSource);
// For each data point, specify a start time, end time, and the data value -- in this case,
// the number of new steps.
        DataPoint dataPoint = dataSet.createDataPoint()
                .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS);
        //dataPoint.getValue(Field.FIELD_CALORIES).setInt(stepCountDelta);
        dataSet.add(dataPoint);
        DataReadResult dataReadResult =
                Fitness.HistoryApi.readData(mClient, readRequest).await(0, TimeUnit.MINUTES);
    }
    private void dumpDataSet(DataSet dataSet) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");

        for (DataPoint dp : dataSet.getDataPoints()) {

            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for(Field field : dp.getDataType().getFields()) {
                /*if (dp.getDataType().equals(DataType.TYPE_ACTIVITY_SEGMENT)) {
                    String val = dp.getValue(field).asActivity();
                    if (!(val.equals("ELEVATOR") || val.equals("ESCALATOR") ||
                            val.substring(0, 4).equals("SLEEP") || val.equals("STILL"))) {
                        Log.i(TAG, "add: " + dp.getValue(field));
                    }*/
                Log.i(TAG, "\tField: " + field.getName() +
                            " Value: " + dp.getValue(field));
                dataList.add(Integer.parseInt(dp.getValue(field).toString()));
                //}
                //else{
                Log.i(TAG, "Field: " + field.getName());
                    //burnedCal+=Integer.parseInt(dp.getValue(field).toString());
                //}
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
