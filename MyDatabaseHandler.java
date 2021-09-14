package com.example.opencv;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDatabaseHandler extends SQLiteOpenHelper {
    private static final String DB_NAME = "VKSFacialImages";

    // below int is our database version
    private static final int DB_VERSION = 1;

    // below variable is for our table name.
    private static final String TABLE_NAME = "ImagePro";

    // below variable is for our id column.
    private static final String ID_COL = "id";

    // below variable is for our course name column
    private static final String FILE_NAME = "FileName";

    // below variable id for our course duration column.
    private static final String IMAGES_COL = "Images";

    // below variable for our course description column.
    private static final String VALUE = "StrValue";

    // below variable is for our course tracks column.
    private static final String FLO_VAL = "Value";

    private static final String DATE_TIME = "DateAndTime";


    // creating a constructor for our database handler.
    public MyDatabaseHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    // below method is for creating a database by running a sqlite query
    @Override
    public void onCreate(SQLiteDatabase db) {
        // on below line we are creating
        // an sqlite query and we are
        // setting our column names
        // along with their data types.
        String query = "CREATE TABLE " + TABLE_NAME + " ("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + FILE_NAME + " TEXT,"
                + IMAGES_COL + " BLOB,"
                + FLO_VAL + " FLOAT,"
                + VALUE + " TEXT,"
                + DATE_TIME + " TEXT)";

        // at last we are calling a exec sql
        // method to execute above sql query
        db.execSQL(query);
    }

    // this method is use to add new course to our sqlite database.
    public boolean insertdetails(String fileName,byte[] images, String  strvalue, Float value, String datetime) {
        try{
            System.out.println("Hi Namu");
            // on below line we are creating a variable for
            // our sqlite database and calling writable method
            // as we are writing data in our database.



            SQLiteDatabase db = this.getWritableDatabase();

            // on below line we are creating a
            // variable for content values.
            ContentValues values = new ContentValues();

            // on below line we are passing all values
            // along with its key and value pair.
            values.put(FILE_NAME, fileName);
            values.put(IMAGES_COL, images );
            values.put(FLO_VAL, value);
            values.put(VALUE, strvalue);
            values.put(DATE_TIME, datetime);


            // after adding all values we are passing
            // content values to our table.
            System.out.println("Hi Nam");
            db.insert(TABLE_NAME, null, values);
            System.out.println("Hi Namrata");
            // at last we are closing our
            // database after adding database.
            db.close();
        }catch(Exception e){
            System.out.println("insertDetails :"+e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this method is called to check if the table exists already.
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

}
