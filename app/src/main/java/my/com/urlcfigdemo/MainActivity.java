package my.com.urlcfigdemo;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.yunjiglobal.binder.UrlBinderF;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HashMap hashMap=new HashMap();
        hashMap.put("test","www.hao123.com");
        hashMap.put("base","www.youtube.com");
        UrlBinderF.bind(IBaseUrl.class,hashMap);
        //UrlBinder.bind(IBaseUrl.class, hashMap);
        Log.i("yuxin",IBaseUrl.BUYER_SERVER_URL);
        Log.i("yuxin",IBaseUrl.BASE_SERVER_URL);
    }
}
