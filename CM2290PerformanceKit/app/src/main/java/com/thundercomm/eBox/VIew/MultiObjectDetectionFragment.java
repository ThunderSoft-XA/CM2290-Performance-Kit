package com.thundercomm.eBox.VIew;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.thundercomm.eBox.Config.GlobalConfig;
import com.thundercomm.eBox.Data.Recognition;
import com.thundercomm.eBox.Model.RtspItemCollection;
import com.thundercomm.eBox.R;

import java.util.HashMap;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MultiObjectDetectionFragment extends PlayFragment {
    private static final String TAG = "MultiObjectDetectionFragment";
    MultiBoxTracker tracker;
    protected Paint paint_Object;
    HashMap<String, Integer> mCurrentNumHashMap = new HashMap<>();
    private ImageView nativeImageView;
    private ImageView superImageView;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_playe, container, false);

        final ImageView superResolutionImageView = view.findViewById(R.id.srimageView);
        final ImageView nativelyScaledImageView = view.findViewById(R.id.lowimageView);
        superImageView = superResolutionImageView;
        nativeImageView = nativelyScaledImageView;
        return view;
    }
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tracker = new MultiBoxTracker(getContext());
        initPaint();
        paint_Object = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint_Object.setColor(Color.CYAN);
        paint_Object.setShadowLayer(10f, 0, 0, Color.CYAN);
        paint_Object.setStyle(Paint.Style.STROKE);
        paint_Object.setStrokeWidth(4);
        paint_Object.setFilterBitmap(true);

        resetCurrentNumHashMap();
    }

    public MultiObjectDetectionFragment(int id) {
        super(id);
    }

    private void draw(final SurfaceHolder mHolder, Bitmap lowBitmap, Bitmap superBitmap) {
        Canvas canvas = null;
        int x = 100;
        int y = 100;
        if (mHolder != null) {
            try {
                canvas = mHolder.lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), E_resource[index_E]);
                int mBitWidth = lowBitmap.getWidth();
                int mBitHeight = lowBitmap.getHeight();
                Rect mSrcRect = new Rect(100, 100, 200, 200);
                Rect mDestRect = new Rect(100, 100, 200, 200);
                canvas.drawBitmap(lowBitmap, mSrcRect, mDestRect, paint_Txt);
//                srimageView lowimageView
//                if (label != null) {
//                    int x = 400;
//                    int y = 150;
//                    int w = 1000;
//                    int h = 700;
//                    Rect rect = new Rect(x, y, x + w, y + h);
//                    drawRound(canvas, x, y, w, h, paint_Object);
//                    Point show_textPoint = get_show_coordinate(mFaceRectView.getWidth(),
//                            mFaceRectView.getHeight(), rect);
//                    canvas.drawText(label, show_textPoint.x, show_textPoint.y, paint_Txt);
//                }



                tracker.setFrameConfiguration(200, 200);
//                tracker.trackResults(results);
                tracker.draw(canvas);
//
//                String msg = "Current Goods Amount:";
//                canvas.drawText(msg, x, y, paint_Txt);

//                for (final Recognition recognition : results) {
//                    mCurrentNumHashMap.put(recognition.getTitle(), mCurrentNumHashMap.get(recognition.getTitle()) + 1);
//                }
//
//                for (String key : mCurrentNumHashMap.keySet()) {
//                    y += 50;
//                    msg = key + ":" + mCurrentNumHashMap.get(key);
//                    canvas.drawText(msg, x, y, paint_Txt);
//                }
//                resetCurrentNumHashMap();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (null != canvas) {
                    mHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
        hasDrawn = false;
    }

    private void resetCurrentNumHashMap() {
        mCurrentNumHashMap.clear();
        for (int i = 0; i < GlobalConfig.mCheckClass.length; i++) {
            String class_type = GlobalConfig.mCheckClass[i];
            mCurrentNumHashMap.put(class_type, 0);
        }
    }
    public void onDraw(Bitmap lowbitmap,Bitmap superBitmap) {
        draw(mFaceViewHolder, lowbitmap,superBitmap);
        hasDrawn = true;
        if (nativeImageView != null && superImageView != null) {
            superImageView.setImageDrawable(null);
            nativeImageView.setImageDrawable(null);
            superImageView.setImageBitmap(superBitmap);
            nativeImageView.setImageBitmap(lowbitmap);
        }
    }

}
