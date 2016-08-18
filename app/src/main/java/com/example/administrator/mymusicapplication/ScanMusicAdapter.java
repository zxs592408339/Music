package com.example.administrator.mymusicapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ScanMusicAdapter extends BaseAdapter{
    List<Music> list = new ArrayList<>();
    LayoutInflater layoutInflater;

    public ScanMusicAdapter(Context context, List<Music> list) {
        layoutInflater = LayoutInflater.from(context);
        this.list = list;
    }


    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = layoutInflater.inflate(R.layout.layout_window_list, null);
            TextView musicNameView = (TextView) convertView.findViewById(R.id.music_mane_window_view);
            TextView musicPathView = (TextView) convertView.findViewById(R.id.music_path_window_view);
            MusicBean musicBean = new MusicBean();
            musicBean.mMusicTitleTxt = musicNameView;
            musicBean.mMusicArtistTxt = musicPathView;
            convertView.setTag(musicBean);
        }
        Music item = (Music)getItem(position);
        MusicBean musicBean = (MusicBean)convertView.getTag();
        musicBean.mMusicTitleTxt.setText(item.getTitle());
        musicBean.mMusicArtistTxt.setText(item.getArtist());
        return convertView;
    }

}
