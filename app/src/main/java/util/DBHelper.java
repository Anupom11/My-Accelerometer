package util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class DBHelper extends SQLiteOpenHelper {

    // declare the database name
    private static final String DATABASE_NAME = "myaccelerometer.db";

    // declare the table name
    private static final String my_acc_data = "my_acc_data";

    //--------------------------------------------------------------------------------------------
    // declare the column names for the table
    private static final String ID = "id";
    private static final String LAT = "lat";
    private static final String LNG = "lng";
    private static final String X_VAL = "x_val";
    private static final String Y_VAL = "y_val";
    private static final String Z_VAL = "z_val";
    private static final String ROAD_NAME = "road_name";
    private static final String VEHICLE_NAME = "vehicle_name";
    private static final String SPEED = "speed";
    private static final String DATE_VAL = "date_val";
    private static final String START_TIME = "start_time";
    private static final String UNIQUE_ID = "unique_id";

    //--------------------------------------------------------------------------------------------

    public DBHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
    }

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    public DBHelper(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory, int version,
                    @Nullable DatabaseErrorHandler errorHandler) {
        super(context, name, factory, version, errorHandler);
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    public DBHelper(@Nullable Context context, @Nullable String name, int version, @NonNull SQLiteDatabase.OpenParams openParams) {
        super(context, name, version, openParams);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // create table for my accelerometer data table
        db.execSQL("create table my_acc_data" +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat text, lng text, x_val text, y_val text, z_val text, road_name text,"+
                "vehicle_name text, speed text, date_val text, start_time text, unique_id text)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS my_acc_data");
        onCreate(db);
    }

    public boolean insertMyAccelerometerData(String lat, String lng, String xVal, String yVal, String zVal, String roadName,
                                             String vehicleName, String speed, String dateVal, String timeVal, String uniqueId) {
        SQLiteDatabase db           = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(LAT, lat);
        contentValues.put(LNG, lng);
        contentValues.put(X_VAL, xVal);
        contentValues.put(Y_VAL, yVal);
        contentValues.put(Z_VAL, zVal);
        contentValues.put(ROAD_NAME, roadName);
        contentValues.put(VEHICLE_NAME, vehicleName);
        contentValues.put(SPEED, speed);
        contentValues.put(DATE_VAL, dateVal);
        contentValues.put(START_TIME, timeVal);
        contentValues.put(UNIQUE_ID, uniqueId);

        db.insert(my_acc_data, null, contentValues);
        return true;
    }

    /**
     * Function to get whole data from table
     */
    public Cursor getAllData() {
        SQLiteDatabase db   = this.getReadableDatabase();
        Cursor cursor       = db.rawQuery("select * from "+my_acc_data, null);
        return cursor;
    }

    /**
     * Function to retrieve data from the table
     */
    public Cursor getMyAccelerometerData(String id) {
        SQLiteDatabase db   = this.getReadableDatabase();
        Cursor cursor       = db.rawQuery("select * from "+my_acc_data+" where id="+id, null);
        return cursor;
    }

    public boolean updateMyAccelerometerData(String id, String lat, String lng, String xVal, String yVal, String zVal, String roadName,
                                             String vehicleName, String speed, String dateVal, String timeVal, String uniqueId) {
        SQLiteDatabase db           = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        contentValues.put(LAT, lat);
        contentValues.put(LNG, lng);
        contentValues.put(X_VAL, xVal);
        contentValues.put(Y_VAL, yVal);
        contentValues.put(Z_VAL, zVal);
        contentValues.put(ROAD_NAME, roadName);
        contentValues.put(VEHICLE_NAME, vehicleName);
        contentValues.put(SPEED, speed);
        contentValues.put(DATE_VAL, dateVal);
        contentValues.put(START_TIME, timeVal);
        contentValues.put(UNIQUE_ID, uniqueId);

        db.update(my_acc_data, contentValues, id+" = ? ", new String[] { id } );

        return true;
    }

    public Integer deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(my_acc_data, "1", null);
        //return db.execSQL("delete from "+my_acc_data);

    }

    public Integer deleteMyAccelerometerData(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        if(id!=null) {
            return db.delete(my_acc_data, id + " = ? ", new String[]{id});
        }

        return null;
    }

    /**
     * Function to empty the records of my_acc_data table
     * @return affected rows
     */
    public Integer emptyCartProductData() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(my_acc_data, null, null);
    }

}