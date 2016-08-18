package com.example.administrator.mymusicapplication;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.thinkcool.circletextimageview.CircleTextImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MusicMainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, View.OnClickListener {
    private List<View> PagerList = new ArrayList<>();
    private List<View> MainPagerList = new ArrayList<>();
    public ListView mMusicListWindowView, mMusicListLayoutView;
    public ScanMusicAdapter adapter;
    public int mCurrentProgress, mCurrentPosition, duration, current = 0, PLAY_MODE = 1;
    public ImageView mPopupWindowBrn, mLatMusicBrn, mPlayAndPauseBrn, mNextMusicBrn, mPlayModeBrn,
            mWindowTitleModeBrn, mMainLatMusicBrn, mMainNextMusicBrn, mMainPlayAndPauseBrn;
    private TextView mMusicNameTxt, mSingerNameTxt, mCurrentTimeTxt, mTotalTimeTxt,
            mWindowTitleNumberTxt, mMainMusicNameTxt, mMainSingerNameTxt;
    private SimpleDateFormat mDataFormat = new SimpleDateFormat("mm:ss");
    private SeekBar mSeekBar, mMainSeekBar;
    public String musicName, singerName;
    private Handler mHandler = new Handler();
    private final int ORDERING_MODEL = 1, ALL_LOOPING_MODEL = 2, RANDOM_MODEL = 3, LOOPING_MODEL = 4;
    private boolean flag = false;
    private PopupWindow popupWindow;
    public View popupContentView;
    private CircleTextImageView mCircleTextImageView, mMainCircleTextImageView;
    private ViewPager mViewPagerView, mMusicMainPagerView;
    public static List<Music> mMediaLists = new ArrayList<>();
    public MusicListLayoutAdapter musicListAdapter;
    private ObjectAnimator animator, mainAnimator;
    private LayoutInflater layoutInflater;

    // TODO: 2016/7/15 绑定服务的并实现服务接口的方法
    private MusicService.MusicPlayerService musicPlayerService;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            musicPlayerService = (MusicService.MusicPlayerService) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicPlayerService = null;
        }
    };

    // TODO: 2016/7/15 广播的接收者
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.example.administrator.LOCALBROADCASTIMG")) {
                isLastMusic();
            } else if (intent.getAction().equals("com.example.administrator.LOCALBROADCAST")) {
                if (musicPlayerService != null) {
                    current = musicPlayerService.ICurrent();
                    playMusic(current);
                }
            } else if (intent.getAction().equals("com.example.administrator.LOCALBROADCASTBIND")) {
                playMusic(current);
            } else if (intent.getAction().equals("com.example.administrator.MUSIC_NOTIFY_PLAYER")) {
                playAndPause();
            } else if (intent.getAction().equals("com.example.administrator.STOP_SERVICE")) {
                stopService();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_main);

        scanFile();
        Intent intent = new Intent(this, MusicService.class);
        startService(intent);
        bindService(intent, serviceConnection, BIND_AUTO_CREATE);
        mMusicMainPagerView = (ViewPager) findViewById(R.id.music_main_pager_view);
        pagerMainAdapters();
        pagerPlayAdapters();
        getWindowBen();
        updateUI();
        imgMainRotation();
        imgRotation();
        getShared();
    }

    // TODO: 2016/7/5 扫描文件并加载音乐
    public void scanFile() {
        // TODO: 2016/7/5 扫描外部内存文件
        Cursor cursor = getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
                MediaStore.Audio.Media.DATA + " like ?",
                new String[]{Environment.getExternalStorageDirectory() + "%"},
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

        // TODO: 2016/7/5 判断文件是否为音乐文件
        assert cursor != null;
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            String isMusic = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC));
            if (isMusic != null && isMusic.equals("")) continue;
            String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
            String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
            String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
            String image = getAlbumImage(cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)));
            int length = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
            if (isRepeat(title, artist)) continue;

            Music music = new Music();
            music.setId(image);
            music.setTitle(title);
            music.setArtist(artist);
            music.setPath(path);
            music.setLength(length);
            mMediaLists.add(music);
        }
    }

    // TODO: 2016/7/5 判断音乐是否重复
    public boolean isRepeat(String title, String artist) {
        for (Music music : mMediaLists) {
            if (title.equals(music.getTitle()) && artist.equals(music.getArtist())) {
                return true;
            }
        }
        return false;
    }

    // TODO: 2016/7/12 获取图片
    public String getAlbumImage(int albumId) {
        String result = "";
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    Uri.parse("content://media/external/audio/albums/"
                            + albumId), new String[]{"album_art"}, null,
                    null, null);
            assert cursor != null;
            for (cursor.moveToFirst(); !cursor.isAfterLast(); ) {
                result = cursor.getString(0);
                break;
            }
        } catch (Exception ignored) {
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }
        return null == result ? null : result;
    }

    // TODO: 2016/7/15 音乐队列的点击事件
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        switch (parent.getId()) {
            case R.id.music_list_window_view:
                current = position;
                flag = true;
                musicPlayerService.IIsFlag(flag);
                musicPlayerService.IPlayMusic(current);
                popupWindow.dismiss();
                break;
            case R.id.music_list_layout_view:
                current = position;
                flag = true;
                musicPlayerService.IIsFlag(flag);
                musicPlayerService.IPlayMusic(current);
                break;
        }
    }

    // TODO: 2016/7/15 各按键的点击事件
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.music_latest_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                if (PLAY_MODE == RANDOM_MODEL)
                    randomModel();
                else
                    playLastMusic();
                break;
            case R.id.music_start_and_pause_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                musicPlayerService.IPlayAndPause();
                playAndPause();
                break;
            case R.id.music_next_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                if (PLAY_MODE == RANDOM_MODEL)
                    randomModel();
                else
                    playNextMusic();
                break;
            case R.id.music_main_latest_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                playLastMusic();
                break;
            case R.id.music_main_start_and_pause_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                musicPlayerService.IPlayAndPause();
                playAndPause();
                break;
            case R.id.music_main_next_brn:
                flag = true;
                musicPlayerService.IIsFlag(flag);
                if (PLAY_MODE == RANDOM_MODEL)
                    randomModel();
                else
                    playNextMusic();
                break;
            case R.id.music_play_mode:
                setPlayMode();
                break;
            case R.id.window_title_mode:
                setPlayMode();
                break;
        }
    }

    // TODO: 2016/7/5 音乐列表弹窗
    public void getWindowBen() {
        layoutInflater = LayoutInflater.from(this);
        popupContentView = layoutInflater.inflate(R.layout.activity_music_list, null);
        mMusicListWindowView = (ListView) popupContentView.findViewById(R.id.music_list_window_view);

        mWindowTitleNumberTxt = (TextView) popupContentView.findViewById(R.id.window_title_number);
        mWindowTitleModeBrn = (ImageView) popupContentView.findViewById(R.id.window_title_mode);
        mMusicListWindowView.setOnItemClickListener(this);
        mWindowTitleModeBrn.setOnClickListener(this);

        adapter = new ScanMusicAdapter(this, mMediaLists);
        mMusicListWindowView.setAdapter(adapter);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int screenHeight = dm.heightPixels - 500;
        int screenWidth = ViewGroup.LayoutParams.WRAP_CONTENT;


        popupWindow = new PopupWindow(this);
        popupWindow.setContentView(popupContentView);

        popupWindow.setWidth(screenWidth);
        popupWindow.setHeight(screenHeight);

        popupWindow.setFocusable(true);
        popupWindow.setTouchable(true);


        mPopupWindowBrn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (popupWindow.isShowing()) {
                    popupWindow.dismiss();
                } else {
                    popupWindow.showAsDropDown(v);
                    mWindowTitleNumberTxt.setText("" + mMediaLists.size());
                }
            }
        });
    }

    // TODO: 2016/7/5 设置显示音乐（音乐名，歌手，图片，时间等）
    public void playMusic(int current) {
        if (mMediaLists.size() != 0) {
            Bitmap mMusicImg = BitmapFactory.decodeFile(mMediaLists.get(current).getImage());
            mCircleTextImageView.setImageBitmap(mMusicImg == null ? BitmapFactory.decodeResource(getResources(),
                    R.drawable.skin_kg_playing_bar_default_avatar) : mMusicImg);
            mMainCircleTextImageView.setImageBitmap(mMusicImg == null ? BitmapFactory.decodeResource(getResources(),
                    R.drawable.skin_kg_playing_bar_default_avatar) : mMusicImg);
            showMusicInformation();
            if (musicPlayerService != null)
                if (musicPlayerService.IIsPlaying()) {
                    animator.start();              //开始动画
                    mainAnimator.start();
                    mPlayAndPauseBrn.setImageResource(R.drawable.bpc);
                    mMainPlayAndPauseBrn.setImageResource(R.drawable.c5g);
                }
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mCurrentProgress = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    musicPlayerService.IMusicSeekTo(mCurrentProgress);
                    mCurrentPosition = musicPlayerService.ICurrentTime();
                    mCurrentTimeTxt.setText(mDataFormat.format(new Date(mCurrentPosition)));
                }
            });
            mMainSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mCurrentProgress = progress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    musicPlayerService.IMusicSeekTo(mCurrentProgress);
                    mCurrentPosition = musicPlayerService.ICurrentTime();
                }
            });

            // TODO: 2016/7/5 设置音乐总时长
            if (musicPlayerService != null)
                duration = musicPlayerService.ITotalTime();
            else
                duration = mMediaLists.get(current).getLength();
            mSeekBar.setMax(duration);
            mMainSeekBar.setMax(duration);
            mTotalTimeTxt.setText(mDataFormat.format(new Date(duration)));

        }
    }

    // TODO: 2016/7/5 更新当前音乐的播放时间
    public void updateUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (musicPlayerService != null) {
                                mCurrentPosition = musicPlayerService.ICurrentTime();
                                mSeekBar.setProgress(mCurrentPosition);
                                mMainSeekBar.setProgress(mCurrentPosition);
                                mCurrentTimeTxt.setText(mDataFormat.format(new Date(mCurrentPosition)));
                            }
                        }
                    });
                    SystemClock.sleep(1000);
                }
            }
        }).start();
    }

    // TODO: 2016/7/5 播放下一首
    public void playNextMusic() {
        musicPlayerService.IPlayNextMusic();
    }

    // TODO: 2016/7/5 播放上一首
    public void playLastMusic() {
        musicPlayerService.IPlayLastMusic();
    }

    // TODO: 2016/7/5 播放和暂停歌曲
    public void playAndPause() {
        if (musicPlayerService.IIsPlaying()) {
            mPlayAndPauseBrn.setImageResource(R.drawable.bpc);
            mMainPlayAndPauseBrn.setImageResource(R.drawable.c5g);
            musicPlayerService.IAgainResetMusic();
            if (animator.isRunning()) {
                animator.resume();//恢复动画
                mainAnimator.resume();
            } else {
                animator.start();
                mainAnimator.start();
            }
        } else {
            mPlayAndPauseBrn.setImageResource(R.drawable.bpd);
            mMainPlayAndPauseBrn.setImageResource(R.drawable.c5b);
            animator.pause();//暂停动画
            mainAnimator.pause();
        }
    }

    // TODO: 2016/7/5 显示歌名和歌手
    public void showMusicInformation() {
        musicName = mMediaLists.get(current).getTitle();
        singerName = mMediaLists.get(current).getArtist();
        mMusicNameTxt.setText(musicName);
        mSingerNameTxt.setText(singerName);
        mMainMusicNameTxt.setText(musicName);
        mMainSingerNameTxt.setText(singerName);
    }

    // TODO: 2016/7/5 随机播放
    public void randomModel() {
        musicPlayerService.IRandomModel();
    }

    // TODO: 2016/7/5 设置播放模式
    public void setPlayMode() {
        Toast toast = null;
        if (PLAY_MODE == ORDERING_MODEL) {
            PLAY_MODE = ALL_LOOPING_MODEL;
            toast = Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT);
        } else if (PLAY_MODE == ALL_LOOPING_MODEL) {
            PLAY_MODE = RANDOM_MODEL;
            toast = Toast.makeText(this, "列表循环", Toast.LENGTH_SHORT);
        } else if (PLAY_MODE == RANDOM_MODEL) {
            PLAY_MODE = LOOPING_MODEL;
            toast = Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT);
        } else if (PLAY_MODE == LOOPING_MODEL) {
            PLAY_MODE = ORDERING_MODEL;
            toast = Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT);
        }
        setModeImg();
        musicPlayerService.IPlayMode(PLAY_MODE);
        toast.setMargin(0f, 0.4f);
        toast.show();
    }

    // TODO: 2016/7/5 设置播放模式图标
    public void setModeImg() {
        if (PLAY_MODE == ORDERING_MODEL) {
            mPlayModeBrn.setImageResource(R.drawable.dtm);
            mWindowTitleModeBrn.setImageResource(R.drawable.dtm);
        } else if (PLAY_MODE == ALL_LOOPING_MODEL) {
            mPlayModeBrn.setImageResource(R.drawable.dtq1);
            mWindowTitleModeBrn.setImageResource(R.drawable.dtq1);
        } else if (PLAY_MODE == RANDOM_MODEL) {
            mPlayModeBrn.setImageResource(R.drawable.dto);
            mWindowTitleModeBrn.setImageResource(R.drawable.dto);
        } else if (PLAY_MODE == LOOPING_MODEL) {
            mPlayModeBrn.setImageResource(R.drawable.dtq);
            mWindowTitleModeBrn.setImageResource(R.drawable.dtq);
        }
    }

    // TODO: 2016/7/15 设置图片转动效果
    public void imgRotation() {
        animator = ObjectAnimator.ofFloat(mCircleTextImageView, "rotation", 0, 360);
        animator.setDuration(7000);
        animator.setInterpolator(new LinearInterpolator());//不停顿
        animator.setRepeatCount(-1);//设置动画重复次数
        animator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
    }
    public void imgMainRotation() {
        mainAnimator = ObjectAnimator.ofFloat(mMainCircleTextImageView, "rotation", 0, 360);
        mainAnimator.setDuration(7000);
        mainAnimator.setInterpolator(new LinearInterpolator());//不停顿
        mainAnimator.setRepeatCount(-1);//设置动画重复次数
        mainAnimator.setRepeatMode(ValueAnimator.RESTART);//动画重复模式
    }

    // TODO: 2016/7/5 再次进入设置上次退出是所保存的记录
    public void getShared() {
        SharedPreferences pref = getSharedPreferences("music_data", MODE_PRIVATE);
        current = pref.getInt("current", 0);
        mCurrentPosition = pref.getInt("mCurrentPosition", 0);
        PLAY_MODE = pref.getInt("PLAY_MODE", 1);
        setModeImg();
        mSeekBar.setProgress(mCurrentPosition);
        mMainSeekBar.setProgress(mCurrentPosition);
        mCurrentTimeTxt.setText(mDataFormat.format(new Date(mCurrentPosition)));
    }

    // TODO: 2016/7/7 音乐播放完成后判断是否为最后一首音乐
    public void isLastMusic() {
        Toast.makeText(this, "播放完成！", Toast.LENGTH_SHORT).show();
        mPlayAndPauseBrn.setImageResource(R.drawable.bpd);
        mMainPlayAndPauseBrn.setImageResource(R.drawable.c5b);
        animator.pause();
        mainAnimator.pause();
    }

    // TODO: 2016/7/15 添加音乐数据的适配器类
    public class MusicListLayoutAdapter extends BaseAdapter {
        List<Music> list = new ArrayList<>();

        public MusicListLayoutAdapter(Context context, List<Music> list) {
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
                convertView = layoutInflater.inflate(R.layout.layout_show_music, null);
                ImageView musicImg = (ImageView) convertView.findViewById(R.id.music_image);
                TextView musicNameView = (TextView) convertView.findViewById(R.id.music_mane_view);
                TextView musicPathView = (TextView) convertView.findViewById(R.id.music_path_view);
                MusicBean musicBean = new MusicBean();
                musicBean.mMusicImg = musicImg;
                musicBean.mMusicTitleTxt = musicNameView;
                musicBean.mMusicArtistTxt = musicPathView;
                convertView.setTag(musicBean);
            }
            Music item = (Music) getItem(position);
            MusicBean musicBean = (MusicBean) convertView.getTag();
            Bitmap mMusicImg = BitmapFactory.decodeFile(item.getImage());
            musicBean.mMusicImg.setImageBitmap(mMusicImg == null ? BitmapFactory.decodeResource(getResources(), R.drawable.skin_kg_playing_bar_default_avatar) : mMusicImg);
            musicBean.mMusicTitleTxt.setText(item.getTitle());
            musicBean.mMusicArtistTxt.setText(item.getArtist());
            return convertView;
        }
    }

    // TODO: 2016/7/15 解析分布局
    public void pagerPlayAdapters() {
        layoutInflater = LayoutInflater.from(this);
        View albumsView = layoutInflater.inflate(R.layout.layout_albums_view, null);
        View lrcView = layoutInflater.inflate(R.layout.layout_lrc_view, null);
        mCircleTextImageView = (CircleTextImageView) albumsView.findViewById(R.id.profile_image);
        PagerList.add(albumsView);
        PagerList.add(lrcView);
        MyPagerViewAdapter adapter = new MyPagerViewAdapter(this, PagerList);
        mViewPagerView.setAdapter(adapter);
    }

    // TODO: 2016/7/15 解析主布局
    public void pagerMainAdapters() {
        layoutInflater = LayoutInflater.from(this);
        View musicList = layoutInflater.inflate(R.layout.layout_music_list, null);
        View musicPlay = layoutInflater.inflate(R.layout.layout_music_play, null);

        mMainMusicNameTxt = (TextView) musicList.findViewById(R.id.main_music_name);
        mMainSingerNameTxt = (TextView) musicList.findViewById(R.id.main_singer_name);
        mMainLatMusicBrn = (ImageView) musicList.findViewById(R.id.music_main_latest_brn);
        mMainNextMusicBrn = (ImageView) musicList.findViewById(R.id.music_main_next_brn);
        mMainPlayAndPauseBrn = (ImageView) musicList.findViewById(R.id.music_main_start_and_pause_brn);
        mMainSeekBar = (SeekBar) musicList.findViewById(R.id.music_play_seek_bar);
        mMainCircleTextImageView = (CircleTextImageView) musicList.findViewById(R.id.music_profile_image);
        mMainLatMusicBrn.setOnClickListener(this);
        mMainNextMusicBrn.setOnClickListener(this);
        mMainPlayAndPauseBrn.setOnClickListener(this);

        mPopupWindowBrn = (ImageView) musicPlay.findViewById(R.id.show_music_list);
        mCurrentTimeTxt = (TextView) musicPlay.findViewById(R.id.music_current_time);
        mTotalTimeTxt = (TextView) musicPlay.findViewById(R.id.music_total_time);
        mMusicNameTxt = (TextView) musicPlay.findViewById(R.id.music_name);
        mSingerNameTxt = (TextView) musicPlay.findViewById(R.id.singer_name);
        mSeekBar = (SeekBar) musicPlay.findViewById(R.id.music_seek_bar);
        mLatMusicBrn = (ImageView) musicPlay.findViewById(R.id.music_latest_brn);
        mPlayAndPauseBrn = (ImageView) musicPlay.findViewById(R.id.music_start_and_pause_brn);
        mNextMusicBrn = (ImageView) musicPlay.findViewById(R.id.music_next_brn);
        mPlayModeBrn = (ImageView) musicPlay.findViewById(R.id.music_play_mode);
        mViewPagerView = (ViewPager) musicPlay.findViewById(R.id.pager_view_brn);
        mLatMusicBrn.setOnClickListener(this);
        mPlayAndPauseBrn.setOnClickListener(this);
        mNextMusicBrn.setOnClickListener(this);
        mPlayModeBrn.setOnClickListener(this);

        mMusicListLayoutView = (ListView) musicList.findViewById(R.id.music_list_layout_view);
        musicListAdapter = new MusicListLayoutAdapter(this, mMediaLists);
        mMusicListLayoutView.setAdapter(musicListAdapter);
        MainPagerList.add(musicList);
        MainPagerList.add(musicPlay);

        MyPagerViewAdapter adapter = new MyPagerViewAdapter(this, MainPagerList);
        mMusicMainPagerView.setAdapter(adapter);

        mMusicListLayoutView.setOnItemClickListener(this);
    }

    // TODO: 2016/7/15 注册广播要实现的方法
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver();
    }

    // TODO: 2016/7/15 注册广播的代码
    public void registerReceiver() {
        IntentFilter intentPlay = new IntentFilter("com.example.administrator.MUSIC_NOTIFY_PLAYER");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentPlay);
        IntentFilter intentImage = new IntentFilter("com.example.administrator.LOCALBROADCASTIMG");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentImage);
        IntentFilter intentFilter = new IntentFilter("com.example.administrator.LOCALBROADCAST");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        IntentFilter intentBind = new IntentFilter("com.example.administrator.LOCALBROADCASTBIND");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentBind);
        IntentFilter intentStopService = new IntentFilter("com.example.administrator.STOP_SERVICE");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentStopService);
    }

    // TODO: 2016/7/15 注销广播的代码
    public void unRegisterLocalReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    // TODO: 2016/7/15 注销广播必须实现的方法
    @Override
    protected void onPause() {
        super.onPause();
        unRegisterLocalReceiver();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    // TODO: 2016/7/15 解除绑定，结束服务，退出程序
    public void stopService() {
        unbindService(serviceConnection);
        stopService(new Intent(this, MusicService.class));
        finish();
    }

}