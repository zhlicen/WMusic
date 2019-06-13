package live.a23333.wmusic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;

/**
 * Created by Samuel Zhou on 2017/6/17.
 */

public class MusicFile{
    public MediaMetadataRetriever md;
    public String filePath;
    public String title;
    public String album;
    public String artist;
    static public Bitmap cover;
    public void loadCover() {
        byte[] data = md.getEmbeddedPicture();
        if (data != null) {
            try {
                if(cover != null && !cover.isRecycled()) {
                    final Bitmap oldCover = cover;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            oldCover.recycle();
                        }
                    }).run();
                    cover = null;
                }
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(data, 0, data.length, opts);
                opts.inSampleSize = getinSampleSize(opts);
                opts.inJustDecodeBounds = false;
                cover = BitmapFactory.decodeByteArray(data, 0, data.length, opts);
            }
            catch(Exception ex){
                cover = null;
            }
        }
        else {
            cover = null;
        }
    }

    private int getinSampleSize(BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        float reqHeight = 400.0f;
        float reqWidth = 400.0f;
        int inSampleSize = 1;

        if (height > width && height>reqHeight) {
            inSampleSize = (int) Math.ceil(height/ reqHeight);
        }
        else if(height<=width && width>reqWidth){
            inSampleSize = (int) Math.ceil( width / reqWidth);
        }
        System.out.println("inSampleSize" + inSampleSize);
        return inSampleSize;
    }
}