package disklrucache.bw.com.bitmapthreecachestudy.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import disklrucache.bw.com.bitmapthreecachestudy.R;
import disklrucache.bw.com.bitmapthreecachestudy.utils.BitmapUtils;
import disklrucache.bw.com.bitmapthreecachestudy.utils.ImageUtils;

public class MainActivity extends AppCompatActivity {

    private String path = "https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1546425859927&di=881e5321abb988b7edf4e14497e428e1&imgtype=0&src=http%3A%2F%2Fd.ifengimg.com%2Fw600%2Fe0.ifengimg.com%2F02%2F2018%2F1124%2F08C498B009ACD7517056D6D54CF4CF2E278C1ED6_size26_w650_h400.jpeg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ImageView iv = findViewById(R.id.iv1);
        final ImageView iv2 = findViewById(R.id.iv2);


        final BitmapUtils bitmapUtils = new BitmapUtils(this);

        final ImageUtils imageUtils = new ImageUtils(this);

        imageUtils.display(iv, path);

        iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                imageUtils.display(iv2, path);
            }
        });


    }


}
