package disklrucache.bw.com.bitmapthreecachestudy.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 1, LruCache内存缓存
 * 2, DiskLruCache磁盘缓存
 * 3, 线程池实现网络请求
 */
public class ImageUtils {

    //文件路径
    private final static String CACHE_DIR = Environment.getExternalStorageDirectory() + "/Pictures/sdcache";

    //图片内存所占用的内存大小
    int maxSize = (int) (Runtime.getRuntime().maxMemory() / 8);

    //创建LruCache对象来管理内存图片
    private LruCache<String, Bitmap> images = new LruCache<String, Bitmap>(maxSize) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };


    private android.os.Handler handler = new android.os.Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case 0:
                    ImageViewBitmap imageViewBitmap = (ImageViewBitmap) msg.obj;
                    //显示网络图片
                    imageViewBitmap.iv_head.setImageBitmap(imageViewBitmap.bitmap);
                    break;
            }


        }
    };
    private Context context;
    private DiskLruCache diskLruCache;
    private final ExecutorService executorService;

    //构造方法
    public ImageUtils(Context context) {
        this.context = context;
        //创建文件
        File file = new File(CACHE_DIR);
        if (!file.exists()) {
            file.mkdir();
        }
        try {
            //创建DiskLruCache对象管理本地图片
            diskLruCache = DiskLruCache.open(file, 1, 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //创建线程池
        executorService = Executors.newFixedThreadPool(5);

    }

    //加载图片方法
    public void display(ImageView iv_head, String path) {

        //获取内存图片
        Bitmap bitmap = loadMemary(path);
        if (bitmap != null) {
            iv_head.setImageBitmap(bitmap);
            Log.i("xxx", "走内存");
        } else {
            //获取本地图片
            bitmap = loadSd(path);

            if (bitmap != null) {
                Log.i("xxx", "sd");
                iv_head.setImageBitmap(bitmap);
            } else {
                //获取网络图片
                loadInternet(iv_head, path);
                Log.i("xxx", "网络");

            }


        }


    }

    //获取内存图片
    private Bitmap loadMemary(String path) {

        Bitmap bitmap = images.get(path);
        if (bitmap != null) {
            return bitmap;
        }
        return null;

    }

    //获取本地图片
    private Bitmap loadSd(String path) {
        try {
            DiskLruCache.Snapshot snapshot = diskLruCache.get(hashKeyForDisk(path));

            if (snapshot != null) {
                Bitmap bitmap = BitmapFactory.decodeStream(snapshot.getInputStream(0));
                //缓存到内存
                images.put(path, bitmap);
                return bitmap;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;


    }

    //获取网络图片
    private void loadInternet(ImageView iv_head, String path) {
        // new Thread(new MyRunnable(iv_head, path)).start();
        //使用线程池代替每次开线程
        executorService.submit(new MyRunnable(iv_head, path));
    }

    //在线程里做耗时操作
    private class MyRunnable implements Runnable {
        ImageView iv_head;
        String path;

        public MyRunnable(ImageView iv_head, String path) {
            this.iv_head = iv_head;
            this.path = path;

        }

        @Override
        public void run() {
            BufferedOutputStream out = null;
            BufferedInputStream in = null;
            try {
                URL url = new URL(path);

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    InputStream inputStream = conn.getInputStream();

                    //存在sd中

                    /*FileOutputStream fos = new FileOutputStream(file);
                    BufferedOutputStream bos = new BufferedOutputStream(fos);
                    //质量压缩
                    bitmap.compress(Bitmap.CompressFormat.PNG, 70, bos);
                    bos.flush();
                    bos.close();*/


                    //存在本地中
                    String key = hashKeyForDisk(path);
                    DiskLruCache.Editor editor = null;

                    editor = diskLruCache.edit(key);
                    if (editor != null) {
                        OutputStream outputStream = editor.newOutputStream(0);
                        in = new BufferedInputStream(inputStream);
                        out = new BufferedOutputStream(outputStream);
                        int b;
                        while ((b = in.read()) != -1) {
                            out.write(b);
                        }

                        editor.commit();

                    }
                    //频繁的flush
                    diskLruCache.flush();
                    in.close();
                    out.close();

                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);


                    ImageViewBitmap imageViewBitmap = new ImageViewBitmap(iv_head, bitmap);
                    Message message = handler.obtainMessage(0, imageViewBitmap);
                    handler.sendMessage(message);

                    //存在内存中
                    images.put(path, bitmap);
                    Log.i("xxx", "保存成功");


                    inputStream.close();

                }


            } catch (Exception e) {
                e.printStackTrace();
            }


        }
    }

    private String getFileName(String path) {

        return path.substring(path.lastIndexOf("/") + 1);
    }

    private class ImageViewBitmap {
        ImageView iv_head;
        Bitmap bitmap;

        public ImageViewBitmap(ImageView iv_head, Bitmap bitmap) {

            this.iv_head = iv_head;
            this.bitmap = bitmap;
        }
    }

    private String hashKeyForDisk(String key) {
        String cacheKey;
        try {
            final MessageDigest mDigest = MessageDigest.getInstance("MD5");
            mDigest.update(key.getBytes());
            cacheKey = bytesToHexString(mDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            cacheKey = String.valueOf(key.hashCode());
        }
        return cacheKey;
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}
