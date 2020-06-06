package com.ghost.recognition;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.ghost.recognition.imgtoword.WebImage;
import com.github.houbb.opencc4j.util.ZhConverterUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Time;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    public  static  final int TAKE_PHOTO=1;
    public static final int ALBUM_RESULT_CODE = 2;
    private  final int REQUEST_EXTERNAL_STORAGE = 1;
    private ImageView picture;
    private Uri ImageUri;
    private ImageButton takePhoto;
    private ImageButton reversePhoto;
    private ImageButton uploadPhoto;
    private Button getImageText;
    private TextView display;
    private Button simpword;
    int count = 1;
    boolean flag = true;
    private static  String msg = "";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        picture = findViewById(R.id.picture);
        takePhoto =  findViewById(R.id.imageButton_tp);
        reversePhoto = findViewById(R.id.imageButton_rs);
        uploadPhoto = findViewById(R.id.imageButton_up);
        getImageText = findViewById(R.id.bt_getword);
        simpword = findViewById(R.id.bt_simptotrad);
        display = findViewById(R.id.display);
        verifyStoragePermissions(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }


        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建File对象，永不存储拍照后的图片
                File outputImage = new File(getExternalCacheDir(),"outputImage.jpg");
                try {
                    if(outputImage.exists()){
                        outputImage.delete();
                    }
                    outputImage.createNewFile();

                }catch (Exception ex){
                    ex.printStackTrace();
                }
                if(Build.VERSION.SDK_INT>=24){
                    ImageUri = FileProvider.
                            getUriForFile(MainActivity.this, "com.example.camerralbumtest.fileprovider",outputImage);
                }else{
                    ImageUri = Uri.fromFile(outputImage);
                }
                //启动相机程序
                //Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                intent.putExtra(MediaStore.EXTRA_OUTPUT,ImageUri);
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA,Manifest.permission.WRITE_EXTERNAL_STORAGE},1);//（询问用户授予权限）
                startActivityIfNeeded(intent,TAKE_PHOTO);
            }
        });

        uploadPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSysAlbum();
            }
        });

        reversePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                picture.setPivotX(picture.getWidth()/2);
//                picture.setPivotY(picture.getHeight()/2);//支点在图片中心
                //  picture.setRotation(degree);
                int i = count++;
                float degree = 90 * i;
                picture.animate().rotation(degree);



               // Toast.makeText(MainActivity.this,"角度："+degree+" 宽度："+picture.getWidth(),Toast.LENGTH_LONG).show();
            }
        });

        simpword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (flag){
                    String original = display.getText().toString();
                    String result = ZhConverterUtil.toTraditional(original);
                    display.setText(result);
                    flag = false;
                }else{
                    String original = display.getText().toString();
                    String result = ZhConverterUtil.toSimple(original);
                    display.setText(result);
                    flag = true;
                }

            }
        });

        getImageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            picture.setDrawingCacheEnabled(true);
                            Matrix mtx = new Matrix();
                            mtx.postRotate(90*(count-1));
                            Bitmap bitmap = Bitmap.createBitmap(picture.getDrawingCache(),0, 0, picture.getWidth(), picture.getHeight(), mtx, true);
                            picture.setDrawingCacheEnabled(false);
                           // Toast.makeText(MainActivity.this,"角度："+count+" 宽度：",Toast.LENGTH_LONG).show();


                            //picture.buildDrawingCache();
                            //Bitmap bitmap = picture.getDrawingCache();
                            //Bitmap bitmap = ((BitmapDrawable)picture.getDrawable()).getBitmap();
                            String path = Environment.getExternalStorageDirectory()+"/DCIM/Camera/";
                            File appDir = new File(Environment.getExternalStorageDirectory(),"/DCIM/Camera/");
                            if(!appDir.exists()){
                                appDir.mkdir();
                            }
                            String fileName = System.currentTimeMillis()+".jpg";
                            //保存照片
                            File file = new File(appDir, fileName);
                            FileOutputStream fos = null; // 创建一个文件输出流对象
                            try {
                                fos = new FileOutputStream(file);
                                bitmap.compress(Bitmap.CompressFormat.JPEG,100,fos);
                                fos.flush();
                                fos.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }catch (IOException e) {
                                e.printStackTrace();
                                //失败的提示
                                Toast.makeText(MainActivity.this,e.getMessage(),Toast.LENGTH_SHORT).show();
                            }

                            //最后通知图库更新
                            Uri uri = Uri.fromFile(file);
                            MainActivity.this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri));
                            //Toast.makeText(this,"照片保存至："+file,Toast.LENGTH_LONG).show();
                            String result = 	WebImage.webImage(path+fileName);
                            if(result!=null){
                                JSONObject obj = JSONObject.parseObject(result);
                                JSONArray jsonArr = obj.getJSONArray("words_result");
                                StringBuilder sb = new StringBuilder();
                                for (Object string : jsonArr) {
                                    Object json = JSONObject.toJSON(string);
                                    JSONObject wordObj = JSONObject.parseObject(JSON.toJSONString(json));
                                    System.out.println(wordObj.get("words").toString());
                                    sb.append(wordObj.get("words").toString()).append("\n");

                                }
                                msg = sb.toString();

                            }else{
                                msg = "抱歉，无法识别文字！";


                            }

                        }
                    }).start();
                display.setText(msg);
            }
        });



    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {
                        //将拍摄的照片显示出来
                        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(ImageUri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case ALBUM_RESULT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    //使用content的接口
                    ContentResolver cr = this.getContentResolver();
                    try {
                        //获取图片
                        Bitmap bitmap = BitmapFactory.decodeStream(cr.openInputStream(uri));
                        picture.setImageBitmap(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;

        }
    }

    private  String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public  void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }

    /**
     * 打开本地相册选择图片
     */
    private void openSysAlbum(){
        Intent albumIntent = new Intent(Intent.ACTION_PICK);
        albumIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        startActivityForResult(albumIntent, ALBUM_RESULT_CODE);


    }
}