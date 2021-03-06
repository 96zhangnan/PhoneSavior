package com.example.inspiron.phonesavior.ui;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.example.inspiron.phonesavior.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.List;

public class CameraActivity extends Activity {

    private SurfaceView mySurfaceView;
    private SurfaceHolder myHolder;
    private Camera myCamera;
    private int num = 0;
    public static WeakReference<CameraActivity> weak = null;
    private boolean isTaking = false;
    private int count = 0;
    private boolean isFocusing = false;

    //  private Camera.AutoFocusCallback myAutoFocusCallback  = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // 无title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // 全屏
     //   getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //设置布局
        setContentView(R.layout.activity_camera);

        Log.d("Demo", "oncreate");

        //初始化surface
        initSurface();

        //这里得开线程进行拍照，因为Activity还未完全显示的时候，是无法进行拍照的，SurfaceView必须先显示
        new Thread(new Runnable() {
            @Override
            public void run() {
                //初始化camera并对焦拍照
                //initCamera();
             //   boolean result = false;

                while(!isTaking && (count <= 10)) {
                    num++;
                    Log.d("Demo", "第"+ num +"次开始拍照" );
                    initCamera();

                    try {
                        Thread.sleep(20 * 1000); //设置暂停的时间 20 秒
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d("Demo", "第"+ num +"次结束拍照" );
                    isTaking = false;

                }

                try {
                    Thread.sleep(5 * 1000); //设置暂停的时间 5 秒
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if(count > 10){
                    Toast.makeText(CameraActivity.this, "检测到您持续用眼，请注意用眼", Toast.LENGTH_SHORT).show();
                }
                CameraActivity.this.finish();
            }
        }).start();

    }

    //初始化surface
    @SuppressWarnings("deprecation")
    private void initSurface()
    {
        //初始化surfaceview
        mySurfaceView = (SurfaceView) findViewById(R.id.camera_surfaceview);

        //初始化surfaceholder
        myHolder = mySurfaceView.getHolder();
        myHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

    }

    //初始化摄像头
    private void initCamera() {

        isTaking = true;

        //如果存在摄像头
        if(checkCameraHardware(getApplicationContext()))
        {
            //获取摄像头（首选前置，无前置选后置）
            if(openFacingFrontCamera())
            {
                Log.d("Demo", "openCameraSuccess");
                //进行对焦
                try {
                    //因为开启摄像头需要时间，这里让线程睡两秒
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                while(isFocusing);
                if(!isFocusing){
                    autoFocus();
                }
            }
            else {
                Log.d("Demo", "openCameraFailed");
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //对焦并拍照
    private void autoFocus() {

        isTaking = true;

        try {
            //因为开启摄像头需要时间，这里让线程睡两秒
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //自动对焦
        myCamera.autoFocus(myAutoFocusCallback);

        try {
            //因为开启摄像头需要时间，这里让线程睡两秒
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //对焦后拍照
        myCamera.takePicture(null, null, myPicCallback);
    }

    //判断是否存在摄像头
    private boolean checkCameraHardware(Context context) {

        isTaking = true;

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // 设备存在摄像头
            return true;
        } else {
            // 设备不存在摄像头
            return false;
        }

    }

    //得到后置摄像头
    private boolean openFacingFrontCamera() {

        isTaking = true;

        //尝试开启前置摄像头
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    Log.d("Demo", "tryToOpenCamera");
                    myCamera = Camera.open(camIdx);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    return false;
                }
            }
        }

        //如果开启前置失败（无前置）则开启后置
        if (myCamera == null) {
            for (int camIdx = 0, cameraCount = Camera.getNumberOfCameras(); camIdx < cameraCount; camIdx++) {
                Camera.getCameraInfo(camIdx, cameraInfo);
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    try {
                        myCamera = Camera.open(camIdx);
                    } catch (RuntimeException e) {
                        return false;
                    }
                }
            }
        }

        try {
            //这里的myCamera为已经初始化的Camera对象
            myCamera.setPreviewDisplay(myHolder);
        } catch (IOException e) {
            e.printStackTrace();
            myCamera.stopPreview();
            myCamera.release();
            myCamera = null;
        }
        myCamera.stopPreview();
        myCamera.startPreview();

        return true;
    }

    //自动对焦回调函数(空实现)
    private Camera.AutoFocusCallback myAutoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            // TODO Auto-generated method stub
            isFocusing = true;
            if(success )//success表示对焦成功
            {
                Log.i("myAutoFocusCallback", " success...");
                //myCamera.setOneShotPreviewCallback(null);
            }
            else {
                //未对焦成功
                Log.i("myAutoFocusCallback", " failed...");
                myCamera.autoFocus(myAutoFocusCallback);
            }
            isFocusing = false;
        }
    };

    //拍照成功回调函数
    private Camera.PictureCallback myPicCallback = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //完成拍照后关闭Activity
            //CameraActivity.this.finish();

            BitmapFactory.Options options = new BitmapFactory.Options();
//               options.inSampleSize =2;
            options.inPreferredConfig = Bitmap.Config.RGB_565;//必须设置为565，否则无法检测

            //将得到的照片进行270°旋转，使其竖直
            Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
            Matrix matrix = new Matrix();
            matrix.preRotate(270);
            bitmap = Bitmap.createBitmap(bitmap ,0,0, bitmap .getWidth(), bitmap .getHeight(),matrix,true);



            //FaceDetecor只能读取RGB 565格式的Bitmap，将上面得到的Bitmap进行一次格式转换
     //       Bitmap myBitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
          //  String oristring = bitmapToString(bitmap);

            //假设最多有1张脸
            int MAXfFaces = 1;
            int numOfFaces = 0;
            FaceDetector mFaceDetector = new FaceDetector(bitmap.getWidth(),bitmap.getHeight(),MAXfFaces);
            FaceDetector.Face[] mFace = new FaceDetector.Face[MAXfFaces];
            //获取实际上有多少张脸
            numOfFaces = mFaceDetector.findFaces(bitmap, mFace);
            Log.v("------------->",  "pic num:" + num + "  face num:"+numOfFaces );
            if(numOfFaces == 1){
                if(mFace[0].eyesDistance() >= 100){
                    count++;
                }
               Log.d("pic num:" + num,  "  eyesDistance:"+ mFace[0].eyesDistance() +"  confidence:"+ mFace[0].confidence());
            }

            //创建并保存图片文件
            File pictureFile = new File(getDir(), "camera"+num+".jpg");
            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
            } catch (Exception error) {
                Toast.makeText(CameraActivity.this, "拍照失败", Toast.LENGTH_SHORT).show();;
                Log.d("Demo", "保存照片失败" + error.toString());
                error.printStackTrace();
               // myCamera.stopPreview();
                //myCamera.release();
                myCamera.startPreview();
                //myCamera = null;
            }

            bitmap.recycle();
            bitmap = null;

            Log.d("Demo", "获取照片成功");
            Toast.makeText(CameraActivity.this, "获取照片成功", Toast.LENGTH_SHORT).show();
          //  myCamera.stopPreview();
            //myCamera.release();
            myCamera.startPreview();
            //myCamera = null;
        }
    };

    public String bitmapToString(Bitmap bitmap){
        //将Bitmap转换成字符串
        String string=null;
        ByteArrayOutputStream bStream=new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,bStream);
        byte[]bytes=bStream.toByteArray();
        string= Base64.encodeToString(bytes,Base64.DEFAULT);
        return string;
    }


/*    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            doTakeAction();
        }
    };*/

    //获取文件夹
    private File getDir()
    {
        //得到SD卡根目录
        File dir = Environment.getExternalStorageDirectory();

        if (dir.exists()) {
            return dir;
        }
        else {
            dir.mkdirs();
            return dir;
        }
    }

    /**
     * 在别的Activity关闭自己的方法
     */
    public static void finishActivity() {
        if (weak!= null && weak.get() != null) {
            weak.get().finish();
        }
    }

}

