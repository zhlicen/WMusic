package live.a23333.wmusic;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class WearFragmentAdapter extends FragmentPagerAdapter {
    static int COUNT = 3;
    private String[] titles;
    private Context context;

    public WearFragmentAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        titles = new String[]{"player", "list", "wifi"};
    }


    @Override
    public Fragment getItem(int position) {
        if(position == 0) {
            return PlayerFragment.newInstance(context);
        }
        else if(position == 1) {
            return FileListFragment.newInstance(context);
        }
        else if(position == 2) {
            return StartWifiFragment.newInstance(context);
        }
        return null;

    }


    @Override
    public int getCount() {
        return COUNT;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titles[position];
    }
}