package live.a23333.wmusic;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

class WFragmentAdapter extends FragmentPagerAdapter {
    public final int COUNT = 2;
    private String[] titles;
    private Context context;

    public WFragmentAdapter(FragmentManager fm, Context context) {
        super(fm);
        this.context = context;
        titles = new String[]{context.getString(R.string.tab_file),
                context.getString(R.string.tab_about)};
    }


    @Override
    public Fragment getItem(int position) {
        if(position == 0){
            return FileListFragment.newInstance(context);
        }
        else if(position == 1){
            return AboutFragment.newInstance(context);
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