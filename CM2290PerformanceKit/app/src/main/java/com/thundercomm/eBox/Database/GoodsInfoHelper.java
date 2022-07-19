package com.thundercomm.eBox.Database;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class GoodsInfoHelper extends SQLiteOpenHelper {

    public static String CREATE_TABLE = "create table "+ DatabaseStatic.TABLE_NAME +"(" +
            DatabaseStatic.ID+" integer primary key autoincrement,"+
            DatabaseStatic.CLASS_TYPE + " text, " +
            DatabaseStatic.CURRENT_NUM + " Integer not null, " +
            DatabaseStatic.SALES + " Integer not null, " +
            DatabaseStatic.VIEW_NUM + " Integer not null, " +
            DatabaseStatic.DATE + " text"
            + ")";


    public GoodsInfoHelper(Context context, String name,
            SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DatabaseStatic.DATABASE_NAME, null, DatabaseStatic.DATABASE_VERSION);
    }

    public GoodsInfoHelper(Context context)
    {
        super(context, DatabaseStatic.DATABASE_NAME, null, DatabaseStatic.DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
    }
}
