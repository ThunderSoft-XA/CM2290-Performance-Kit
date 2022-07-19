package com.thundercomm.eBox.Database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.thundercomm.eBox.Activity.MainActivity;
import com.thundercomm.eBox.Config.GlobalConfig;
import com.thundercomm.eBox.Data.Recognition;
import com.thundercomm.eBox.VIew.MultiBoxTracker;
import com.thundercomm.eBox.VIew.MultiObjectDetectionFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class GoodsManager {

    private static String TAG = "GoodsManager";
    private static final Object syncObj = new Object();
    private static GoodsManager instance;
    private GoodsInfoHelper mGoodsInfoHelper;

    HashMap<String, Integer> mLastNumHashMap = new HashMap<>();
    HashMap<String, Integer> mCurrentNumHashMap = new HashMap<>();
    HashMap<String, Boolean> isMissingHashMap = new HashMap<>();
    HashMap<String, Long> lastMissTimeStampHashMap = new HashMap<>();
    HashMap<String, Integer> missNumHashMap = new HashMap<>();
    public static GoodsManager getInstance(Context context) {
        if (instance == null) {
            synchronized (syncObj) {
                instance = new GoodsManager(context);
            }
        }
        return instance;
    }

    public GoodsManager(Context context) {
        mGoodsInfoHelper = new GoodsInfoHelper(context);

        resetCurrentNumHashMap();
        mLastNumHashMap.putAll(mCurrentNumHashMap);
    }


    public void checkResults(final List<Recognition> mappedRecognitions, MultiObjectDetectionFragment frgment) {

        for (final Recognition recognition : mappedRecognitions) {
            mCurrentNumHashMap.put(recognition.getTitle(), mCurrentNumHashMap.get(recognition.getTitle()) + 1);
        }
        checkClassNum(frgment);

        for (String key : mCurrentNumHashMap.keySet()) {

            boolean isSold = false;
            boolean isView = false;

            int lastNum  = mLastNumHashMap.get(key);
            if(mCurrentNumHashMap.get(key) < lastNum) {
                isMissingHashMap.put(key, true);
                lastMissTimeStampHashMap.put(key, System.currentTimeMillis());
                missNumHashMap.put(key, lastNum);
                showToast("One " + key + " is taken away", frgment);
            }
            if (isMissingHashMap.get(key) != null &&  isMissingHashMap.get(key) == true) {
                if (missNumHashMap.get(key) <= mCurrentNumHashMap.get(key)) {
                    isMissingHashMap.put(key, false);
                    missNumHashMap.put(key, 0);
                    isView = true;
                    showToast("One " + key + " is put back", frgment);
                } else if (System.currentTimeMillis() - lastMissTimeStampHashMap.get(key) >= 5000){
                    isSold = true;
                    isMissingHashMap.put(key, false);
                    missNumHashMap.put(key, 0);
                    lastMissTimeStampHashMap.put(key, 0L);
                    showToast("One " + key + " is sold", frgment);
                }
            }

            mLastNumHashMap.put(key, mCurrentNumHashMap.get(key));
            insertOrUpdateDatabase(key, mCurrentNumHashMap.get(key), isSold, isView);

        }
        resetCurrentNumHashMap();
    }

    private void resetCurrentNumHashMap() {
        mCurrentNumHashMap.clear();
        for (int i = 0; i < GlobalConfig.mCheckClass.length; i++) {
            String class_type = GlobalConfig.mCheckClass[i];
            mCurrentNumHashMap.put(class_type, 0);
        }
    }

    private void checkClassNum(MultiObjectDetectionFragment frgment) {

        for (int i = 0; i < GlobalConfig.mCheckClass.length; i++) {
            String class_type = GlobalConfig.mCheckClass[i];
            if (mCurrentNumHashMap.get(class_type) == 0) {
                showToast(class_type + " may be sold out, please add in time", frgment);
            }
        }
    }

    private void showToast(String message, MultiObjectDetectionFragment frgment) {
        frgment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast = Toast.makeText(frgment.getContext(), message,
                        Toast.LENGTH_SHORT);
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.show();
            }
        });
    }

    private void insertOrUpdateDatabase(String  classType, int current_num, boolean isSold, boolean isView)
    {
        int sales = 0;
        int view_num = 0;
        SQLiteDatabase database = mGoodsInfoHelper.getWritableDatabase();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date(System.currentTimeMillis());
        simpleDateFormat.format(date);
        String sDate = simpleDateFormat.format(date);
        Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME +
                        " where "+ DatabaseStatic.DATE + "=?" + " and " + DatabaseStatic.CLASS_TYPE + "=?"
                , new String[] {sDate, classType});
        if(cursor.moveToFirst()) {
            sales = isSold ? cursor.getInt(3) + 1: cursor.getInt(3);
            view_num = isView ? cursor.getInt(4) + 1: cursor.getInt(4);
            updateDatabase(classType, current_num, sales, view_num, sDate);

        } else {
            insertDatabase(classType, current_num, sDate);
        }

        cursor.close();
    }

    private void insertDatabase(String classType, int current_num, String sDate) {
        SQLiteDatabase database = mGoodsInfoHelper.getWritableDatabase();

        ContentValues cV = new ContentValues();
        cV.put(DatabaseStatic.CLASS_TYPE, classType);
        cV.put(DatabaseStatic.CURRENT_NUM, current_num);
        cV.put(DatabaseStatic.SALES, 0);
        cV.put(DatabaseStatic.VIEW_NUM, 0);
        cV.put(DatabaseStatic.DATE, sDate);
        database.insert(DatabaseStatic.TABLE_NAME, null, cV);

    }

    private void updateDatabase(String classType, int current_num, int sales, int view_num, String date) {
        SQLiteDatabase database = mGoodsInfoHelper.getWritableDatabase();

        ContentValues cV = new ContentValues();
        cV.put(DatabaseStatic.CURRENT_NUM, current_num);
        cV.put(DatabaseStatic.SALES, sales);
        cV.put(DatabaseStatic.VIEW_NUM, view_num);
        database.update(DatabaseStatic.TABLE_NAME, cV, DatabaseStatic.DATE + "= ?" +
                " and " + DatabaseStatic.CLASS_TYPE + "=?", new String[]{date, classType});
    }
}

