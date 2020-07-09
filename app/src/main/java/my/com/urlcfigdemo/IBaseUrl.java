package my.com.urlcfigdemo;

import com.yunjiglobal.annotations.UrlConfig;

public class IBaseUrl {
    @UrlConfig("test")
    public static String BUYER_SERVER_URL ="1111";
    @UrlConfig("base")
    public static String BASE_SERVER_URL ="1111";
}
