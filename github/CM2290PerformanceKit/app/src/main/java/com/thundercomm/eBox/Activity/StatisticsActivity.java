package com.thundercomm.eBox.Activity;

import java.util.ArrayList;
import java.util.List;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.thundercomm.eBox.Data.GoodsInfo;
import com.thundercomm.eBox.Database.GoodsInfoHelper;
import com.thundercomm.eBox.Database.DatabaseStatic;
import com.thundercomm.eBox.R;

public class StatisticsActivity extends AppCompatActivity {

    private GoodsInfoHelper mGoodsInfoHelper;
    private SQLiteDatabase database = null;
    private Spinner dateSpinner;
    private List<String> data_list = new ArrayList<String>();;
    private ArrayAdapter<String> arr_adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        dateSpinner = (Spinner) findViewById(R.id.date_spinner);
        data_list = getDatabase();
        arr_adapter= new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data_list);
        dateSpinner.setAdapter(arr_adapter);
        dateSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String itemString = dateSpinner.getItemAtPosition(position).toString();
                draw(itemString);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // TODO Auto-generated method stub
            }
        });
    }

    void draw(String date) {
        BarChart twoBarChart = (BarChart) findViewById(R.id.bar_chart);
        List<GoodsInfo> GoodsInfoList = getGoodsInfoList(date);

        final List<String> xList = new ArrayList<String>();
        for (int i = 0; i < GoodsInfoList.size(); i++) {
            xList.add(i, GoodsInfoList.get(i).class_type);
        }
        twoBarChart.setNoDataText("No Data");
        twoBarChart.animateXY(1000, 1000);
        twoBarChart.getDescription().setEnabled(false);

        XAxis xAxis = twoBarChart.getXAxis();
        xAxis.setAxisMinimum(0f);
        xAxis.setAxisMaximum(xList.size());
        xAxis.setLabelCount(xList.size(),false);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setCenterAxisLabels(true);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return xList.get((int) Math.abs(value) % xList.size());
            }
        });

        YAxis rightYAxis = twoBarChart.getAxisRight();
        rightYAxis.setEnabled(false);
        YAxis leftYAxis = twoBarChart.getAxisLeft();
        leftYAxis.enableGridDashedLine(10f, 10f, 0f);

        List<IBarDataSet> dataSets = new ArrayList<>();
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            entries.add(new BarEntry(i, GoodsInfoList.get(i).sales_num));
        }
        BarDataSet barDataSet = new BarDataSet(entries, "Goods Sales Volume");
        barDataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                    ViewPortHandler viewPortHandler) {
                return (int) value + "";
            }
        });
        barDataSet.setColor(Color.BLUE);
        dataSets.add(barDataSet);

        List<BarEntry> entries2 = new ArrayList<>();
        for (int i = 0; i < xList.size(); i++) {
            entries2.add(new BarEntry(i, GoodsInfoList.get(i).view_num));
        }
        BarDataSet barDataSet2 = new BarDataSet(entries2, "Goods View Times");
        barDataSet2.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex,
                    ViewPortHandler viewPortHandler) {
                return (int) value + "";
            }
        });
        barDataSet2.setColor(Color.RED);
        dataSets.add(barDataSet2);

        BarData data = new BarData(dataSets);

        int barAmount = dataSets.size();

        float groupSpace = 0.3f;
        float barSpace = 0.05f;
        float barWidth = (1f - groupSpace) / barAmount - 0.05f;

        data.setBarWidth(barWidth);
        data.groupBars(0f, groupSpace, barSpace);

        twoBarChart.setData(data);
    }

    private List<String> getDatabase()
    {

        if(mGoodsInfoHelper == null) {
            mGoodsInfoHelper = new GoodsInfoHelper(this);
        }
        List<String> dates = new ArrayList<>();

        database = mGoodsInfoHelper.getWritableDatabase();
        Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME ,null);

        while (cursor.moveToNext()) {
            dates.add(cursor.getString(5));//date
        }
        removeDuplicate(dates);
        cursor.close();
        return dates;
    }

    private static void removeDuplicate(List<String> list) {
        List<String> result = new ArrayList<String>(list.size());
        for (String str : list) {
            if (!result.contains(str)) {
                result.add(str);
            }
        }
        list.clear();
        list.addAll(result);
    }


    private List<GoodsInfo> getGoodsInfoList(String date) {
        if(mGoodsInfoHelper == null) {
            mGoodsInfoHelper = new GoodsInfoHelper(this);
        }
        List<GoodsInfo> GoodsInfoList = new ArrayList<>();

        Cursor cursor = database.rawQuery("select * from " + DatabaseStatic.TABLE_NAME +
                " where "+ DatabaseStatic.DATE + "=?", new String[] {date});

        while (cursor.moveToNext()) {
            GoodsInfo goodsInfo = new GoodsInfo();
            goodsInfo.class_type = cursor.getString(1);
            goodsInfo.current_num = cursor.getInt(2);
            goodsInfo.sales_num = cursor.getInt(3);
            goodsInfo.view_num = cursor.getInt(4);
            GoodsInfoList.add(goodsInfo);
        }

        cursor.close();
        return GoodsInfoList;
    }
}

