package disklrucache.bw.com.bitmapthreecachestudy.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;


/**
 * 1, LruCache内存缓存
 * 2, sd磁盘缓存
 * 3, 线程实现网络请求
 */
public class BitmapUtils {
    //图片sd缓存目录
    private final static String SD_CACHE_DIR = Environment.getExternalStorageDirectory() + "/Pictures";

    //指定一个图片缓存的内存大小

    int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);

    //定义一个强引用来缓存图片

    private LruCache<String, Bitmap> mMemoryImages = new LruCache<String, Bitmap>(maxSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };


    private Handler handler = new Handler();
    private Context context;

    public BitmapUtils(Context context) {
        this.context = context;

    }


    //加载图片 做三级缓存
    public void dispaly(ImageView iv, String path) {

        //先从内存取图片

        Bitmap bitmap = getMemory(path);
        if (bitmap != null) {
            iv.setImageBitmap(bitmap);
            Log.i("xxx", "走内存了");
        } else {
            //从sd取图片
            bitmap = getSd(path);   //1、根据url获取文件名字  2、根据文件路径加载图片（1、设置Option的采样模式为只采宽高的模式 2、第一次采样获取图片的宽高3、根据图片和控件的宽高计算缩放比例4、将缩放比例设置给option同时关闭option的只采宽高的模式 5根据这个option第二次采样，这次采的就是缩放后的图片）
            if (bitmap != null) {
                iv.setImageBitmap(bitmap);
                Log.i("xxx", "走sd了");
            } else {
                //从网络取图片

                getInternet(iv, path);
                Log.i("xxx", "走网络了");

            }

        }


    }


    //从内存取图片
    private Bitmap getMemory(String path) {
        Bitmap bitmap = mMemoryImages.get(path);
        if (bitmap != null) {
            return bitmap;
        }
        return null;

    }

    private Bitmap getSd(String path) {
        //从sd中获取
        String fileName = getFileName(path);

        File file = new File(SD_CACHE_DIR, fileName);

        //进行二次采样
        //只加载图片宽高  第一次采样
        BitmapFactory.Options options = new BitmapFactory.Options();
        //开启只获取宽高模式
        options.inJustDecodeBounds = true;
        //把开启了只获取宽和高的option传给decodeFile方法
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        //图片的宽和高复制给了options的 outWidth  和 outHeight
        int outWidth = options.outWidth;
        int outHeight = options.outHeight;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int scale = 0;
        //计算图片的宽度和控件的宽度比例
        int scaleX = outWidth / width;
        int scaleY = outHeight / height;

        //计算压缩比率

        scale = scaleX > scaleY ? scaleX : scaleY;

        //第二次采样
        //关闭只采样宽高模式
        options.inJustDecodeBounds = false;
        //给option设置最终的缩放比列
        options.inSampleSize = scale;
        //设置图片的质量
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        //获取到的最终的Bitmap就是缩放后一定比例的bitmap
        Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);


        if (bitmap != null) {

            //从sd取出来再缓存到内存
            mMemoryImages.put(path, bitmap);
            return bitmap;
        }


        return null;


    }

    //从网络取图片
    private void getInternet(ImageView iv, String path) {

        new Thread(new MyRunnable(iv, path)).start();

}

    //定义一个Runnable内部类
    private class MyRunnable implements Runnable {
        ImageView iv;
        String path;

        public MyRunnable(ImageView iv, String path) {
            this.iv = iv;
            this.path = path;


        }

        @Override
        public void run() {

            try {
                URL url = new URL(path);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(5000);
                conn.setRequestMethod("GET");
                if (conn.getResponseCode() == 200) {
                    InputStream inputStream = conn.getInputStream();
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    BitmapFactory.Options option = new BitmapFactory.Options();
                    //第一次采样  采宽高
                    BitmapFactory.decodeStream(inputStream,null,option);
                    int outWidth = option.outWidth;
                    int outHeight = option.outHeight;

                    int scale = outHeight/iv.getWidth();

                    option.inSampleSize=scale;
                    option.inJustDecodeBounds=false;
                    Bitmap bitmap1 = BitmapFactory.decodeStream(inputStream, null, option);


                    //缓存到sd中
                    //根据url获取文件名字
                    String fileName = getFileName(path);
                    //获取和该url图片对应的文件的对象
                    File file = new File(SD_CACHE_DIR, fileName);
                    //根据文件获取对应文件的流
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fileOutputStream);

                    //将bitmap存储到SD中 ， compress方法作用： 将bitmap写入到流中，这个流对应的是SD卡上的一个文件
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos);

                    //缓存到内存 只要sd里面有图片，就不会走网络了，所以内存当中就没有图片
                    mMemoryImages.put(path, bitmap);

                    //把iv,bitmap封装成一个对象
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            iv.setImageBitmap(bitmap);
                        }
                    });

                }


            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    //获取图片的名字
    private String getFileName(String path) {


        return path.substring(path.lastIndexOf("/") + 1);

    }


    //把iv,bitmap封装成一个对象
    private class ImageViewBitmap {
        ImageView iv;
        Bitmap bitmap;

        public ImageViewBitmap(ImageView iv, Bitmap bitmap) {
            this.iv = iv;
            this.bitmap = bitmap;

        }
    }

}
