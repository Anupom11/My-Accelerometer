package com.lasa.mynotes;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import util.DBHelper;
import util.WriteCSVFile;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private int REQUEST_CODE_PERMISSIONS = 1001;
    private int REQUEST_CHECK_SETTINGS = 1002;
    private final String[] REQUIRED_PERMISSIONS = new String[] {
            //"android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
    };

    private String latitude = "", longitude = "", speedVal="";

    String roadNameVal = "", vehicleNameVal = "";

    String currentDateVal = "", currentTimeValue = "";
    String uniqueIdForDataSet = "";

    private Location currentLocation;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    EditText roadName, vehicleName;
    Button startFetching, getDataList, deleteDataList;
    Button fetchLocationBtn;
    TextView speedValue;

    LinearLayout dataLayoutSection;

    //----------------------------------
    TextView latTextView, longTextView, xTextView, yTextView, zTextView;
    //----------------------------------
    TextView currentLatTV, currentLongTV;
    ImageView backButton;
    Context context;
    DBHelper dbHelper;

    boolean dataFetchingFlag = false;

    WriteCSVFile writeCSVFile = new WriteCSVFile();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        currentDateVal = "";        // reset the current date value
        currentTimeValue = "";      // reset the current time value
        uniqueIdForDataSet = "";    // reset the unique id value

        //------------------------------------------------------------------------------
        dbHelper = new DBHelper(getApplicationContext());

        // Initialize SensorManager
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Initialize accelerometer sensor
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        //------------------------------------------------------------------------------

        //--------------------------------------------------------------------------------------------------
        if(allPermissionsGranted()) {
            if(checkGpsStatus()) {
                startLocService();
            }
            else {
                //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                promptGPSEnableOp();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "GPS is not on. Please turn on the GPS", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
        //--------------------------------------------------------------------------------------------------

        roadName = findViewById(R.id.note_header);
        vehicleName = findViewById(R.id.note_body);

        //speedValue = findViewById(R.id.speed_value);

        startFetching = findViewById(R.id.start_fetching);

        fetchLocationBtn = findViewById(R.id.fetch_loc);

        currentLatTV = findViewById(R.id.lat_text);
        currentLongTV = findViewById(R.id.long_text);

        backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        fetchLocationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getCurrentLoc();
            }
        });

        startFetching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the user input data
                roadNameVal = roadName.getText().toString();
                vehicleNameVal = vehicleName.getText().toString();

                // get the user lat and long value
                String latVal = currentLatTV.getText().toString().trim();
                String longVal = currentLongTV.getText().toString().trim();

                if(latVal.length() == 0 || longVal.length() == 0) {
                    Toast.makeText(getApplicationContext(), "Please fetch your current location at first!", Toast.LENGTH_LONG).show();
                }
                else {
                    // get the current date value
                    Date c = Calendar.getInstance().getTime();
                    SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                    currentDateVal = df.format(c);

                    // write the data to the table
                    if(dbHelper.insertMyAccelerometerData(String.valueOf(latVal), String.valueOf(longVal), "NA", "NA", "NA",
                            roadNameVal, vehicleNameVal, speedVal, currentDateVal, "NA", uniqueIdForDataSet)) {
                        Toast.makeText(getApplicationContext(), "Data saved successfully.", Toast.LENGTH_LONG).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "Failed to save data.", Toast.LENGTH_LONG).show();
                    }
                }

            }
        });

    }
    //*** end of oncreate method ***

    public void getCurrentLoc() {
        if(allPermissionsGranted()) {
            if(checkGpsStatus()) {
                startLocService();
            }
            else {
                //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                promptGPSEnableOp();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "GPS is not on. Please turn on the GPS", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    // method to do the permission request and GPS enable operations
    public boolean doPermissionOperation() {
        if(allPermissionsGranted()) {
            if(checkGpsStatus()) {
                startLocService();

                return true;
            }
            else {
                //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                promptGPSEnableOp();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "GPS is not on. Please turn on the GPS", Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        return false;
    }


    // method to stop the writing data operation
    public void stopDataFetchingOp() {
        // reset the data to stop the db write operation
        roadNameVal = "";
        vehicleNameVal = "";
        currentDateVal = "";
        currentTimeValue = "";
        uniqueIdForDataSet = "";

        dataFetchingFlag = false;

        dataLayoutSection.setBackgroundColor(Color.parseColor("#FEF7FF"));

        //-------------------------------------------
        // reset the value in the screen for view
        latTextView.setText("Lat: ");
        longTextView.setText("Long: ");
        xTextView.setText("X: ");
        yTextView.setText("Y: ");
        zTextView.setText("Z: ");
        //-------------------------------------------

        startFetching.setText("Start");

    }

    // method to fetch data from the sqlite db and write to a file
    public void doFileExportOp() {

        String newFileName = writeCSVFile.getCsvFileName();

        Handler handler = new Handler(Looper.getMainLooper());

        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            Cursor cursor = dbHelper.getAllData();
            cursor.moveToFirst();

            FileOutputStream fos = null;
            BufferedWriter bw = null;

            try {
                // Open file output stream
                fos = new FileOutputStream(new File(newFileName));
                bw = new BufferedWriter(new OutputStreamWriter(fos));

                // CSV header
                String csvHeader = "id, lat, long, x, y, z, rname, vname, speed, date, stime, uid";

                bw.write(csvHeader);
                bw.newLine();

                if (cursor.moveToFirst()) {
                    do {
                        String id           = cursor.getString(cursor.getColumnIndexOrThrow ("id"));
                        String lat          = cursor.getString(cursor.getColumnIndexOrThrow ("lat"));
                        String lng          = cursor.getString(cursor.getColumnIndexOrThrow ("lng"));
                        String xVal         = cursor.getString(cursor.getColumnIndexOrThrow ("x_val"));
                        String yVal         = cursor.getString(cursor.getColumnIndexOrThrow ("y_val"));
                        String zVal         = cursor.getString(cursor.getColumnIndexOrThrow ("z_val"));
                        String roadName     = cursor.getString(cursor.getColumnIndexOrThrow ("road_name"));
                        String vehicleName  = cursor.getString(cursor.getColumnIndexOrThrow ("vehicle_name"));
                        String speed        = cursor.getString(cursor.getColumnIndexOrThrow("speed"));
                        String dateVal      = cursor.getString(cursor.getColumnIndexOrThrow ("date_val"));
                        String startTime    = cursor.getString(cursor.getColumnIndexOrThrow("start_time"));
                        String uniqueID     = cursor.getString(cursor.getColumnIndexOrThrow ("unique_id"));

                        // Write data
                        bw.write(id +","+ lat +","+ lng +","+ xVal +","+ yVal +","+ zVal +","+ roadName +","+ vehicleName +","+ speed +","+ dateVal +","+ startTime +","+ uniqueID);
                        bw.newLine();

                    } while (cursor.moveToNext());
                }

                // Flush and close the writer
                bw.flush();

            } catch (IOException e) {
                e.printStackTrace();  // Handle IO exceptions
            } finally {
                // Close resources
                try {
                    if (bw != null) {
                        bw.close();
                    }
                    if (fos != null) {
                        fos.close();
                    }
                    if (cursor != null) {
                        cursor.close();  // Close the cursor
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            handler.post(() -> {
                System.out.println("CSV file written successfully to " + newFileName);
                Toast.makeText(getApplicationContext(), "CSV file written successfully to " + newFileName, Toast.LENGTH_SHORT).show();
            });

        }).start();

    }
    //*** end of method ***

    public void doAccelerometerWork() {
        // Register the listener for the accelerometer
        if (accelerometer != null) {
            /*HandlerThread handlerThread = new HandlerThread("SensorThread");
            handlerThread.start();
            Handler handler = new Handler(handlerThread.getLooper());

            sensorManager.registerListener(this , accelerometer, SensorManager.SENSOR_DELAY_NORMAL, handler);*/

            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Check if the sensor type is the accelerometer
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Get accelerometer values
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Do something with the accelerometer data
            //System.out.println("Accelerometer readings: X: " + x + " Y: " + y + " Z: " + z + "Lat:"+latitude+" :: Long:"+longitude+" :: speedVal:"+speedVal);

            //speedValue.setText(speedVal+" km/h");

            if(latitude !="" && longitude != "" && roadNameVal != "" && vehicleNameVal!= "" && currentDateVal != "" && currentTimeValue != "" && uniqueIdForDataSet != "") {

                System.out.println("Fetching");

                // set the data section background
                dataLayoutSection.setBackgroundColor(Color.parseColor("#fc627f"));

                //-------------------------------------------
                // put the value in the screen for view
                /*latTextView.setText("Lat: "+latitude);
                longTextView.setText("Long: "+longitude);
                xTextView.setText("X: "+x);
                yTextView.setText("Y: "+y);
                zTextView.setText("Z: "+z);*/
                //-------------------------------------------

                Long tsLong = System.currentTimeMillis()/1000;  // get the time stamp

                // write the data to the table
                dbHelper.insertMyAccelerometerData(String.valueOf(latitude), String.valueOf(longitude), String.valueOf(x), String.valueOf(y), String.valueOf(z),
                        roadNameVal, vehicleNameVal, speedVal, currentDateVal, String.valueOf(tsLong), uniqueIdForDataSet);

            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    // end of oncreate method

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener to save battery
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //-----------------------------------------
        // code section for location service
        if(allPermissionsGranted()) {
            if(checkGpsStatus()) {
                startLocService();
            }
        }
        //-----------------------------------------

        // Re-register the listener when the activity is resumed
        if (accelerometer != null) {
            //sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // handle the GPS turn on operation
    private void promptGPSEnableOp() {
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setTitle("Info");
        alertBuilder.setMessage("You have to turn on the location...")
                .setCancelable(false)
                .setPositiveButton("Turn on", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));

                        Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent1);

                        dialogInterface.cancel();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        /*if(timer1!=null)
                            timer1.cancel();*/

                        dialogInterface.cancel();

                        finish();
                    }
                });

        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    public boolean checkGpsStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        assert locationManager != null;
        boolean GpsStatus = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return GpsStatus;
    }

    private boolean allPermissionsGranted() {
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // checking for location permission
        if (requestCode == REQUEST_CODE_PERMISSIONS) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (allPermissionsGranted()) {
                    if(checkGpsStatus()) {
                        startLocService();
                    }
                    else {
                        //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                        promptGPSEnableOp();
                    }
                } else {
                    Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_LONG).show();
                    //this.finish();
                }
            } else {
                // Permission was denied, show a message or take appropriate action
                Toast.makeText(this, "Location permission denied! To enable go to the app's setting and grant the permission.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // The user enabled the location settings, start location updates
            } else {
                // The user didn't enable the location settings
                Toast.makeText(this, "Location not enabled, can't get coordinates", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void startLocService() {

        //-------------------------------------------------------------------------------------------------------------------------
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new MyLocationListener();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        //-------------------------------------------------------------------------------------------------------------------------

    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            //------------------------------------
            currentLocation = loc;
            //------------------------------------

            speedVal = String.valueOf(loc.getSpeed());

            longitude   = String.valueOf(loc.getLongitude());
            latitude    = String.valueOf(loc.getLatitude());

            //System.out.println("Lat:"+latitude+" :: Long:"+longitude);

            // set the current lat and long in the text box
            currentLatTV.setText(latitude);
            currentLongTV.setText(longitude);

            //------- To get city name from coordinates --------
            /*String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (addresses.size() > 0) {
                    System.out.println(addresses.get(0).getLocality());
                    cityName = addresses.get(0).getLocality();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }

            String s = longitude + "\n" + latitude + "\n\nMy Current City is: " + cityName;*/

        }

        @Override
        public void onProviderDisabled(String provider) {}

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}
    }



}