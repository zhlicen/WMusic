package live.a23333.wmusic;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import android.media.MediaMetadataRetriever;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.util.Random;


/**
 * Created by zhlic on 5/6/2017.
 */

public class WPlayList {

    protected class FileExtensionFilter implements FilenameFilter {
        public boolean accept(File dir, String name) {
            // name = StringUtils.toUtf8(name);
            Log.d("WPlayList", "Checking : " + name);
            name = name.toLowerCase();
            return (name.endsWith("wav") || name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith("ogg") ||
            name.endsWith("aac") || name.endsWith("flac"));
        }
    }

    protected boolean mShuffle = false;
    public int currentIndex = -1;

    public void setShuffle(boolean shuffle){
        mShuffle = shuffle;
        if(shuffle) {
            generateShuffleList();
        }
        else {
            shuffleList.clear();
        }
    }

    public void generateShuffleList() {
        Random rnd = new Random();
        int size = fileList.size();
        if(size == 0) {
            return;
        }
        for(int i = 0; i < size; i++) {
            if(i == currentIndex) {
                continue;
            }
            shuffleList.add(i);
        }
        for(int i = shuffleList.size() - 1; i > 0; i--) {
            int index = rnd.nextInt(i + 1);
                // Simple swap
            int fileIdx = shuffleList.get(index);
            shuffleList.set(index, shuffleList.get(i));
            shuffleList.set(i, fileIdx);
        }
        if(currentIndex != -1) {
            shuffleList.add(currentIndex);
        }
        shuffleListPos = shuffleList.size() - 1;
    }

    public MusicFile getNextFile(){
        if(fileList.isEmpty()){
            return null;
        }
        int nextIndex;
        if(mShuffle){
            nextIndex = getShuffleIndex(false);
        }
        else {
            nextIndex = currentIndex + 1;
            if(nextIndex == fileList.size()){
                nextIndex = 0;
             }
        }
        return getFileByIndex(nextIndex);
    }

    public MusicFile getFileByName(String name, boolean setCurrent){
        if(fileList.isEmpty()){
            return null;
        }
        for(int i = 0; i < fileList.size(); i++) {
            MusicFile file = fileList.get(i);
            if(file.title.equals(name)) {
                if(setCurrent) {
                    currentIndex = i;
                }
                return file;
            }
        }
        return null;
    }


    public MusicFile getPreFile(){
        if(fileList.isEmpty()) {
            return null;
        }
        if(mShuffle) {
            int preIndex = getShuffleIndex(true);
            return getFileByIndex(preIndex);
        }
        else {
            if(currentIndex == -1) {
                return null;
            }
            if(currentIndex == 0) {
                return getFileByIndex(fileList.size() - 1);
            }
            return getFileByIndex(currentIndex - 1);
        }
    }

    public MusicFile getFileByIndex(int index){
        if(index < fileList.size()){
            if(mShuffle) {
                playHistory.add(index);
            }
            currentIndex = index;
            return fileList.get(index);
        }
        return null;
    }

    public MusicFile getCurrentFile(){
        if(fileList.isEmpty() || currentIndex == -1){
            return null;
        }
        return fileList.get(currentIndex);
    }

    int shuffleListPos = -1;
    protected int getShuffleIndex(boolean pre){
        if(shuffleList.size() == 0) {
            shuffleListPos = -1;
            return -1;
        }
        if(pre) {
            if(shuffleListPos < 0) {
                shuffleListPos = 0;
            } else if(shuffleListPos == 0) {
                shuffleListPos = shuffleList.size() - 1;
            }
            else {
                shuffleListPos = shuffleListPos - 1;
            }
        }
        else {
            shuffleListPos = shuffleListPos + 1;
            if(shuffleListPos >= shuffleList.size()) {
                shuffleListPos = 0;
            }
        }
        return shuffleList.get(shuffleListPos);
    }




    public List<MusicFile> fileList;
    List<Integer> playHistory;
    List<Integer> shuffleList;

    public WPlayList(){
        fileList = new ArrayList<>();
        playHistory = new ArrayList<>();
        shuffleList = new ArrayList<>();

    }

    protected void doLoad(String path) {
        loadMusicList(path);
        sortMusicList();
        Log.d("WPlayerList", fileList.toString());
    }

    class MusicFileComparator implements Comparator<MusicFile> {
        @Override
        public int compare(MusicFile o1, MusicFile o2) {
            return o1.title.compareToIgnoreCase(o2.title);
        }
    }
    protected void sortMusicList() {
        MusicFile[] fileArray = new MusicFile[fileList.size()];
        fileList.toArray(fileArray);
        Arrays.sort(fileArray, new MusicFileComparator());
        fileList = Arrays.asList(fileArray);
    }


    protected void loadMusicList(String path){
        try {
            File f = new File(path);
            File files[] = f.listFiles(new FileExtensionFilter());
            if (files.length == 0) {
                return;
            }
            for (File file : files) {
                if (file.isDirectory()) {
                    loadMusicList(file.getPath());
                } else {
                    if(!file.exists()) {
                        Log.d("WPlayList", file.getName() + " ignored");
                        continue;
                    }
                    fileList.add(newMusicFile(file));
                }
            }
        }
        catch(Exception ex) {

        }
    }

    protected MusicFile newMusicFile(File file){
        Log.d("WPlayList", "Add : " + file.getName());
        Log.d("WPlayList", "Path : " + file.getPath());
        MusicFile musicFile = new MusicFile();
        musicFile.filePath = file.getPath();

        MediaMetadataRetriever md = new MediaMetadataRetriever();
        md.setDataSource(file.getPath());
        musicFile.md = md;
        musicFile.title = md.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        musicFile.album = md.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
        musicFile.artist = md.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);

        if(musicFile.title == null){
            musicFile.title = file.getName();
            //StringUtils.toUtf8(file.getName());
        }
        if(musicFile.album == null){
            musicFile.album = "unknown";
        }
        if(musicFile.artist == null){
            musicFile.artist = "unknown";
        }

        return musicFile;
    }
}
