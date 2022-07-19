package com.thundercomm.eBox.AI;

import android.app.Application;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.Image;
import android.os.SystemClock;
import android.widget.Toast;

import androidx.annotation.WorkerThread;

import com.thundercomm.eBox.Config.GlobalConfig;
import com.thundercomm.eBox.Constants.Constants;
import com.thundercomm.eBox.Data.Recognition;
import com.thundercomm.eBox.Jni;
import com.thundercomm.eBox.Model.RtspItemCollection;
import com.thundercomm.eBox.Utils.LogUtil;
import com.thundercomm.eBox.VIew.MultiObjectDetectionFragment;
import com.thundercomm.eBox.VIew.PlayFragment;
import com.thundercomm.gateway.data.DeviceData;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.metadata.MetadataExtractor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import lombok.SneakyThrows;

/**
 * Age Gender Detector
 *
 * @Describe
 */
public class MultiObjectDetectionTask {

    static {
        System.loadLibrary("SuperResolution");
    }
    private static String TAG = "SuperResolution";
    private static final String MODEL_NAME = "ESRGAN.tflite";
    private static final int LR_IMAGE_HEIGHT = 50;
    private static final int LR_IMAGE_WIDTH = 50;
    private static final int UPSCALE_FACTOR = 4;
    private static final int SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR;
    private static final int SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR;
    private MappedByteBuffer model;
    private HashMap<Integer, Interpreter> mapMultiObjectDetection = new HashMap<Integer, Interpreter>();

    private HashMap<Integer, DataInputFrame> inputFrameMap = new HashMap<Integer, DataInputFrame>();
    private Vector< MultiObjectDetectionTaskThread> mMultiObjectDetectionTaskThreads = new Vector<MultiObjectDetectionTaskThread>();

    private boolean istarting = false;
    private boolean isInit = false;
    private Application mContext;
    private ArrayList<PlayFragment> playFragments;

    private int frameWidth;
    private int frameHeight;

    private static volatile MultiObjectDetectionTask _instance;

    private Bitmap selectedLRBitmap = null;

    private MultiObjectDetectionTask() {
    }

    public static MultiObjectDetectionTask getMultiObjectDetectionTask() {
        if (_instance == null) {
            synchronized (MultiObjectDetectionTask.class) {
                if (_instance == null) {
                    _instance = new MultiObjectDetectionTask();
                }
            }
        }
        return _instance;
    }

    public void init(Application context, Vector<Integer> idlist, ArrayList<PlayFragment> playFragments, int width, int height) {
        LogUtil.d(TAG, "init AI");
        frameWidth = width;
        frameHeight = height;
        interrupThread();
        for (int i = 0; i < idlist.size(); i++) {
            if (getMultiObjectDetectionAlgorithmType(idlist.elementAt(i))) {
                DataInputFrame data = new DataInputFrame(idlist.elementAt(i));
                inputFrameMap.put(idlist.elementAt(i), data);
            }
        }
        mContext = context;
        istarting = true;
        isInit = true;
        this.playFragments = playFragments;
        for (int i = 0; i < idlist.size(); i++) {
            if (getMultiObjectDetectionAlgorithmType(idlist.elementAt(i))) {
                MultiObjectDetectionTaskThread multiObjectDetectionThreadTaskThread = new MultiObjectDetectionTaskThread(idlist.elementAt(i));
                multiObjectDetectionThreadTaskThread.start();
                mMultiObjectDetectionTaskThreads.add(multiObjectDetectionThreadTaskThread);
            }
        }
    }

    private boolean getMultiObjectDetectionAlgorithmType(int id) {
        DeviceData deviceData = RtspItemCollection.getInstance().getDeviceList().get(id);
        boolean enable = Boolean.parseBoolean(RtspItemCollection.getInstance().getAttributesValue(deviceData, Constants.ENABLE_MUTILOBJECTDETECTION_STR));
        return enable;
    }

    public void addImgById(int id, final Image img) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.addImgById(img);
    }

    public void addBitmapById(int id, final Bitmap bmp, int w, int h) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.org_w = w;
        data.org_h = h;
        data.addBitMapById(bmp);
    }

    public void addMatById(int id, final Mat img, int w, int h) {
        if (!inputFrameMap.containsKey(id)) {
            return;
        }

        DataInputFrame data = inputFrameMap.get(id);
        data.org_w = w;
        data.org_h = h;
        data.addMatById(img);
    }


    class MultiObjectDetectionTaskThread extends Thread {

        private MultiObjectDetectionFragment multiObjectDetectionTask = null;

        private List<String> GoodsList = Arrays.asList(GlobalConfig.mCheckClass);

        private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
        private static final String TF_OD_API_LABELS_FILE = "labelmap.txt";

        // Only return this many results.
        private static final int NUM_DETECTIONS = 10;
        // Number of threads in the java app
        private static final int NUM_THREADS = 4;
        private boolean isModelQuantized = true;
        // Config values.
        private int inputSize = 300;
        // Pre-allocated buffers.
        private final List<String> labels = new ArrayList<>();
        private int[] intValues;
        // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
        // contains the location of detected boxes
        private float[][][] outputLocations;
        // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
        // contains the classes of detected boxes
        private float[][] outputClasses;
        // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
        // contains the scores of detected boxes
        private float[][] outputScores;
        // numDetections: array of shape [Batchsize]
        // contains the number of detected boxes
        private float[] numDetections;

        private ByteBuffer imgData;

        private Interpreter tfLite;

        private long superResolutionNativeHandle = 0;
        int alg_camid = -1;


        private static final int MODEL_INPUT_SIZE = 300;

        //private Detector detector;
        private Bitmap croppedBitmap;
        private Matrix frameToCropTransform;
        private Matrix cropToFrameTransform;

        protected int previewWidth = 0;
        protected int previewHeight = 0;

        MultiObjectDetectionTaskThread(int id) {

            int cropSize = MODEL_INPUT_SIZE;

            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

            alg_camid = id;

            imgData = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3);
            imgData.order(ByteOrder.nativeOrder());
            intValues = new int[inputSize * inputSize];

            outputLocations = new float[1][NUM_DETECTIONS][4];
            outputClasses = new float[1][NUM_DETECTIONS];
            outputScores = new float[1][NUM_DETECTIONS];
            numDetections = new float[1];

            if (!mapMultiObjectDetection.containsKey(alg_camid)) {
                try {
                    MappedByteBuffer modelFile = loadModelFile();
                    model = modelFile;
                    MetadataExtractor metadata = new MetadataExtractor(modelFile);
                    Interpreter.Options options = new Interpreter.Options();
                    options.setNumThreads(NUM_THREADS);
                    options.setUseXNNPACK(true);
                    options.setUseNNAPI(true);
                    tfLite = new Interpreter(modelFile, options);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mapMultiObjectDetection.put(alg_camid, tfLite);
            } else {
                tfLite = mapMultiObjectDetection.get(alg_camid);
            }
        }

        @SneakyThrows
        @Override
        public void run() {
            super.run();
            Jni.Affinity.bindToCpu(alg_camid % 4 + 4);
            multiObjectDetectionTask = (MultiObjectDetectionFragment) playFragments.get(alg_camid);
            DataInputFrame inputFrame = inputFrameMap.get(alg_camid);
            Mat rotateimage = new Mat(frameHeight, frameWidth, CvType.CV_8UC4);
            Mat resizeimage = new Mat(LR_IMAGE_HEIGHT, LR_IMAGE_WIDTH, CvType.CV_8UC4);
            Mat frameBgrMat = new Mat(frameHeight, frameWidth, CvType.CV_8UC3);
            LogUtil.d("", "debug test start camid  " + alg_camid);

            while (istarting) {
                try {
                    inputFrame.updateFaceRectCache();
                    Mat mat = inputFrame.getMat();

                    if (!OPencvInit.isLoaderOpenCV() || mat == null) {
                        if (mat != null) mat.release();
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    previewWidth = mat.width();
                    previewHeight = mat.height();

                    Core.flip(mat, rotateimage, 0);
                    Imgproc.resize(rotateimage, resizeimage, new Size(LR_IMAGE_HEIGHT, LR_IMAGE_WIDTH));
                    Imgproc.cvtColor(resizeimage, frameBgrMat, Imgproc.COLOR_BGRA2BGR);



                    Bitmap bitmap = Bitmap.createBitmap(LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, Bitmap.Config.ARGB_4444);

                    Utils.matToBitmap(frameBgrMat, bitmap);
                    selectedLRBitmap = bitmap;
                    if (selectedLRBitmap == null) {
                        showToast("no image get!");
                        return;
                    }
                    if (superResolutionNativeHandle == 0) {
                        superResolutionNativeHandle = initTFLiteInterpreter(true);
                    }
                    if (superResolutionNativeHandle == 0) {
                        showToast("TFLite interpreter failed to create!");
                        return;
                    }
                    int[] lowResRGB = new int[LR_IMAGE_HEIGHT * LR_IMAGE_WIDTH];
                    selectedLRBitmap.getPixels(
                            lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT);

                    final long startTime = SystemClock.uptimeMillis();
                    int[] superResRGB = doSuperResolution(lowResRGB);
                    final long processingTimeMs = SystemClock.uptimeMillis() - startTime;
                    if (superResRGB == null) {
                        showToast("Super resolution failed!");
                        return;
                    }

                    Bitmap srImgBitmap =
                            Bitmap.createBitmap(
                                    superResRGB, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                    Bitmap xxx= srImgBitmap;
                    postObjectDetectResult(selectedLRBitmap,srImgBitmap);

                } catch (final Exception e) {
                    e.printStackTrace();
                    LogUtil.e(TAG, "Exception!");
                }
            }
        }

        private void postObjectDetectResult(Bitmap lowbitmap,Bitmap superbitmap) {
            if (multiObjectDetectionTask != null) {
                multiObjectDetectionTask.onDraw(lowbitmap,superbitmap);
            }
        }


        @WorkerThread
        public synchronized int[] doSuperResolution(int[] lowResRGB) {
            return superResolutionFromJNI(superResolutionNativeHandle, lowResRGB);
        }

        private MappedByteBuffer loadModelFile() throws IOException {
            try (AssetFileDescriptor fileDescriptor =
                         AssetsUtil.getAssetFileDescriptorOrCached(mContext.getApplicationContext(), MODEL_NAME);
                 FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor())) {
                FileChannel fileChannel = inputStream.getChannel();
                long startOffset = fileDescriptor.getStartOffset();
                long declaredLength = fileDescriptor.getDeclaredLength();
                return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
            }
        }

        private void showToast(String str) {
            Toast.makeText(mContext.getApplicationContext(), str, Toast.LENGTH_LONG).show();
        }

        private long initTFLiteInterpreter(boolean useGPU) {
            return initWithByteBufferFromJNI(model, useGPU);
        }

        private void deinit() {
            deinitFromJNI(superResolutionNativeHandle);
        }



    }


    public native int[] superResolutionFromJNI(long superResolutionNativeHandle, int[] lowResRGB);

    public native long initWithByteBufferFromJNI(MappedByteBuffer modelBuffer, boolean useGPU);

    public native void deinitFromJNI(long superResolutionNativeHandle);
    public void closeService() {

        isInit = false;
        istarting = false;

        System.gc();
        System.gc();
    }

    private void interrupThread() {
        for (MultiObjectDetectionTaskThread multiObjectDetectionTaskThread : this.mMultiObjectDetectionTaskThreads) {
            if (multiObjectDetectionTaskThread != null && !multiObjectDetectionTaskThread.isInterrupted()) {
                multiObjectDetectionTaskThread.interrupt();
            }
        }
        mapMultiObjectDetection.clear();
    }

    public boolean isIstarting() {
        return isInit;
    }


//    private MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
//            throws IOException {
//        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
//        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
//        FileChannel fileChannel = inputStream.getChannel();
//        long startOffset = fileDescriptor.getStartOffset();
//        long declaredLength = fileDescriptor.getDeclaredLength();
//        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
//    }
}
