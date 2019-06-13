package live.a23333.wmusic;

/**
 * Created by Samuel Zhou on 2017/6/16.
 */

import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;

public class StringUtils {

    static CharsetEncoder asciiEncoder =
            Charset.forName("US-ASCII").newEncoder();
    static CharsetEncoder gb2312Encoder =
            Charset.forName("GB2312").newEncoder();
    static CharsetEncoder big5Encoder =
            Charset.forName("BIG5").newEncoder();
    static CharsetEncoder utf8Encoder =
            Charset.forName("UTF-8").newEncoder();
    static CharsetEncoder utf16Encoder =
            Charset.forName("UTF-16").newEncoder();

    public static String toUtf8(String v) {
        CharsetEncoder encoder = null;
        if(utf8Encoder.canEncode(v)) {
            return v;
        }
        else if(utf16Encoder.canEncode(v)) {
            Log.d("StringUitls", "utf16-encoding");
            encoder = utf16Encoder;
        }
        else if(gb2312Encoder.canEncode(v)) {
            Log.d("StringUitls", "gb2312-encoding");
            encoder = gb2312Encoder;
        }
        else if(big5Encoder.canEncode(v)) {
            Log.d("StringUitls", "big5-encoding");
            encoder = big5Encoder;
        }
        else if(asciiEncoder.canEncode(v)) {
            Log.d("StringUitls", "ascii-encoding");
            encoder = asciiEncoder;
        }
        if(encoder != null) {
            try {
                String s = new String(v.getBytes(encoder.charset()), "UTF-8");
                return s + encoder.charset().toString();
            }
            catch (UnsupportedEncodingException ex) {

            }
        }
        return v;

    }
}