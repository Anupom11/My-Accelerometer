package com.lasa.myaccelerometer;

import androidx.annotation.NonNull;
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
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import util.DBHelper;
import util.WriteCSVFile;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private int REQUEST_CODE_PERMISSIONS = 1001;
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
    TextView speedValue;

    LinearLayout dataLayoutSection;

    //----------------------------------
    TextView latTextView, longTextView, xTextView, yTextView, zTextView;
    //----------------------------------

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
                startLocService();      //start camera if permission has been granted by user
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

        roadName = findViewById(R.id.road_name);
        vehicleName = findViewById(R.id.vehicle_name);

        speedValue = findViewById(R.id.speed_value);

        startFetching = findViewById(R.id.start_fetching);

        getDataList = findViewById(R.id.get_data);
        deleteDataList = findViewById(R.id.delete_data);

        dataLayoutSection = findViewById(R.id.data_layout);

        latTextView     = findViewById(R.id.lat_textview);
        longTextView    = findViewById(R.id.long_textview);
        xTextView       = findViewById(R.id.x_textview);
        yTextView       = findViewById(R.id.y_textview);
        zTextView       = findViewById(R.id.z_textview);

        startFetching.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get the user input data
                roadNameVal = roadName.getText().toString();
                vehicleNameVal = vehicleName.getText().toString();

                if(!dataFetchingFlag) {
                    if (roadNameVal.length() != 0 && vehicleNameVal.length() != 0) {
                        //Toast.makeText(MainActivity.this, "Data:"+roadNameVal+"::"+vehicleNameVal, Toast.LENGTH_SHORT).show();

                        //---------------------------------------------------------------------------------------
                        // get the current date value
                        Date c = Calendar.getInstance().getTime();
                        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
                        currentDateVal = df.format(c);

                        // get the current time value
                        // Get the calendar instance
                        Calendar calendar = Calendar.getInstance();

                        // Extract the current hour, minute, and second
                        currentTimeValue = calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE)+":"+calendar.get(Calendar.SECOND);
                        //---------------------------------------------------------------------------------------

                        //----------------------------------------
                        // get the unique id value
                        UUID uuid = UUID.randomUUID();
                        uniqueIdForDataSet = uuid.toString();
                        //----------------------------------------

                        doAccelerometerWork();

                        // turn on the fetching flag
                        dataFetchingFlag = true;

                        startFetching.setText("Stop");

                    } else {
                        Toast.makeText(MainActivity.this, "Please enter the required details!", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    stopDataFetchingOp();
                }
            }
        });

        getDataList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!dataFetchingFlag) {
                    getAllData();
                }
                else {
                    Toast.makeText(getApplication(), "Please stop the operation before download!", Toast.LENGTH_LONG).show();
                }
            }
        });

        deleteDataList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!dataFetchingFlag) {

                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Confirm")
                            .setMessage("Do you really want to delete all data?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    int countVal = dbHelper.deleteAllData();
                                    Toast.makeText(getApplicationContext(), countVal+" no of records deleted!", Toast.LENGTH_SHORT).show();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
                else {
                    Toast.makeText(getApplication(), "Please stop the operation before deleting data!", Toast.LENGTH_LONG).show();
                }
            }
        });

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

    // method to retrieve data from the db
    public void getAllData() {

        ArrayList<String> dataSetArrList = new ArrayList<>();

        Cursor cursor = dbHelper.getAllData();
        cursor.moveToFirst();

        while(!cursor.isAfterLast()) {
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

            //System.out.println("Data::"+id+"::"+lat+"::"+lng+"::"+xVal+"::"+yVal+"::"+zVal+"::"+roadName+"::"+vehicleName+"::"+dateVal+"::"+uniqueID);

            dataSetArrList.add(id+","+lat+","+lng+","+xVal+","+yVal+","+zVal+","+roadName+","+vehicleName+","+speed+","+dateVal+","+startTime+","+uniqueID);

            cursor.moveToNext();

        }

        String [] dataSetArray = dataSetArrList.toArray(new String[dataSetArrList.size()]);

        //System.out.println("DataArr::"+dataSetArray.toString());
        writeCSVFile.writeCsvFile(getApplicationContext(), dataSetArray);

    }

    public void doAccelerometerWork() {
        // Register the listener for the accelerometer
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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

            speedValue.setText(speedVal+" km/h");

            if(latitude !="" && longitude != "" && roadNameVal != "" && vehicleNameVal!= "" && currentDateVal != "" && currentTimeValue != "" && uniqueIdForDataSet != "") {

                System.out.println("Fetching");

                // set the data section background
                dataLayoutSection.setBackgroundColor(Color.parseColor("#fc627f"));

                //-------------------------------------------
                // put the value in the screen for view
                latTextView.setText("Lat: "+latitude);
                longTextView.setText("Long: "+longitude);
                xTextView.setText("X: "+x);
                yTextView.setText("Y: "+y);
                zTextView.setText("Z: "+z);
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
        // Re-register the listener when the activity is resumed
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
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

                        /*Intent intent1 = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent1);*/

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

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                if(checkGpsStatus()) {
                    startLocService(); //start camera if permission has been granted by user
                }
                else {
                    //Toast.makeText(getApplicationContext(), "Turn on the GPS", Toast.LENGTH_LONG).show();
                    promptGPSEnableOp();
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                this.finish();
            }
        }
        else {
            Toast.makeText(this, "Something went wrong!", Toast.LENGTH_LONG).show();
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

            System.out.println("Lat:"+latitude+" :: Long:"+longitude);

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