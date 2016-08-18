package com.example.administrator.mymusicapplication;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.RemoteViews;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Random;

public class MusicService extends Service {
    private MediaPlayer mMediaPlayer;
    List<Music> mMediaLists = MusicMainActivity.mMediaLists;
    private NotificationManager mNotificationManager;
    private int mCurrentPosition, current = 0, PLAY_MODE = 1;
    public final int ORDERING_MODEL = 1, ALL_LOOPING_MODEL = 2, RANDOM_MODEL = 3, LOOPING_MODEL = 4;
    public String path;
    boolean flag = false;
    private Music mMusic;
    private MusicPlayerBinder musicPlayerBinder = new MusicPlayerBinder();
    private BroadcastReceiver musicReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.example.administrator.MUSIC_NOTIFY_PLAYER")) {
                flag = true;
                musicPlayerBinder.IPlayAndPause();
                Intent intentPlay = new Intent("com.example.administrator.MUSIC_NOTIFY_PLAYER");
                LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intentPlay);
            } else if (intent.getAction().equals("com.example.administrator.MUSIC_NOTIFY_LAST")) {
                flag = true;
                playLastMusic();
            } else if (intent.getAction().equals("com.example.administrator.MUSIC_NOTIFY_NEXT")) {
                flag = true;
                playNextMusic();
            } else if (intent.getAction().equals("com.example.administrator.MUSIC_NOTIFY_EXIT")) {
                Intent intentStopService = new Intent("com.example.administrator.STOP_SERVICE");
                LocalBroadcastManager.getInstance(MusicService.this).sendBroadcast(intentStopService);
                mNotificationManager.cancel(11);
                stopSelf();
            }
        }
    };

    interface MusicPlayerService {
        void IIsFlag(boolean isFlag);

        boolean IIsPlaying(); //是否正在播放

        void IPlayAndPause();    //播放暂停音乐

        int ICurrentTime();  //当前播放时间

        int ITotalTime();  //播放总时间

        void IPlayMusic(int current);

        void IPlayNextMusic();

        void IPlayLastMusic();

        void IRandomModel();

        void IAgainResetMusic();

        void IPlayMode(int playMode);

        int ICurrent();

        void IMusicSeekTo(int mCurrentProgress);
    }

    class MusicPlayerBinder extends Binder implements MusicPlayerService {
        @Override
        public void IIsFlag(boolean isFlag) {
            flag = isFlag;
        }

        @Override
        public boolean IIsPlaying() {
            return mMediaPlayer.isPlaying();
        }

        @Override
        public void IPlayAndPause() {
            customeNotify();
            if (IIsPlaying()) {
                mMediaPlayer.pause();
            } else {
                mMediaPlayer.start();
            }
        }

        @Override
        public int ICurrentTime() {
            return mMediaPlayer.getCurrentPosition();
        }

        @Override
        public int ITotalTime() {
            return mMediaPlayer.getDuration();
        }

        @Override
        public void IPlayMusic(int current) {
            playMusic(current);
        }

        @Override
        public void IPlayNextMusic() {
            playNextMusic();
        }

        @Override
        public void IPlayLastMusic() {
            playLastMusic();
        }

        @Override
        public void IRandomModel() {
            randomModel();
        }

        @Override
        public void IAgainResetMusic() {
            if (current == mMediaLists.size()) {
                current = 0;
                mCurrentPosition = 0;
                playMusic(current);
            }
        }

        @Override
        public void IPlayMode(int playMode) {
            PLAY_MODE = playMode;
        }

        @Override
        public int ICurrent() {
            return current;
        }

        @Override
        public void IMusicSeekTo(int mCurrentProgress) {
            mMediaPlayer.seekTo(mCurrentProgress);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicPlayerBinder;
    }

    @Override
    public void onCreate() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        getShared();
        registerReceiver();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sendLocalBind();
        sendLocalReceiver();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        setShared();
        if (mMediaPlayer != null) {
            mMediaPlayer.release();

            mMediaPlayer = null;
        }
        super.onDestroy();
    }

    // TODO: 2016/7/13 实例化音乐并播放
    public void playMusic(int ICurrent) {
        current = ICurrent;
        mMusic = mMediaLists.get(current);
        path = mMusic.getPath();  // TODO: 2016/7/6 设置音乐路径
        try {
            if (mMediaPlayer == null)
                mMediaPlayer = new MediaPlayer();
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepare();
            customeNotify();
            sendLocalReceiver();
            if (flag) {
                mMediaPlayer.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: 2016/7/5 设置音乐播放模式
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playMode();
            }
        });
    }

    // TODO: 2016/7/5 播放模式
    public void playMode() {
        switch (PLAY_MODE) {
            case ORDERING_MODEL:
                orderModel();
                break;
            case ALL_LOOPING_MODEL:
                allLoopingModel();
                break;
            case RANDOM_MODEL:
                randomModel();
                break;
            case LOOPING_MODEL:
                singleModel();
                break;
        }
    }

    // TODO: 2016/7/5 随机播放
    public void randomModel() {
        int number = mMediaLists.size();
        Random random = new Random();
        current = random.nextInt(number);
        playMusic(current);
    }

    // TODO: 2016/7/5 单曲循环
    public void singleModel() {
        playMusic(current);
    }

    // TODO: 2016/7/5 顺序播放
    public void orderModel() {
        if (++current >= mMediaLists.size()) {
            sendLocalImage();
            current = 0;
            flag = false;
            playMusic(current);
        } else {
            playMusic(current);
        }
    }

    // TODO: 2016/7/5 列表循环
    public void allLoopingModel() {
        if (++current == mMediaLists.size()) {
            current = 0;
        }
        playMusic(current);
    }

    // TODO: 2016/7/5 播放下一首
    public void playNextMusic() {
        current++;
        if (current >= mMediaLists.size()) {
            current = 0;
        }
        playMusic(current);
    }

    // TODO: 2016/7/5 播放上一首
    public void playLastMusic() {
        current--;
        if (current < 0) {
            current = mMediaLists.size() - 1;
        }
        playMusic(current);
    }

    // TODO: 2016/7/5 再次进入设置上次退出时所保存的记录
    public void getShared() {
        SharedPreferences pref = getSharedPreferences("music_data", MODE_PRIVATE);
        current = pref.getInt("current", 0);
        playMusic(current);
        mCurrentPosition = pref.getInt("mCurrentPosition", 0);
        mMediaPlayer.seekTo(mCurrentPosition);
    }

    // TODO: 2016/7/15 发送本地广播
    public void sendLocalReceiver() {
        Intent intent = new Intent("com.example.administrator.LOCALBROADCAST");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void sendLocalImage() {
        Intent intent = new Intent("com.example.administrator.LOCALBROADCASTIMG");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
    public void sendLocalBind() {
        Intent intent = new Intent("com.example.administrator.LOCALBROADCASTBIND");
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }

    // TODO: 2016/7/15 发送通知
    public void customeNotify() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.layout_notification);// TODO: 2016/7/15 解析布局
        builder.setContent(remoteViews);// TODO: 2016/7/15  加载在通知上
        builder.setSmallIcon(R.drawable.image42);// TODO: 2016/7/15 设置通知在状态栏的图标
        builder.setTicker("音乐播放器通知");// TODO: 2016/7/15 设置通知在状态栏显示的信息

        // TODO: 2016/7/15 播放暂停点击事件并发广播
        Intent intentPlay = new Intent("com.example.administrator.MUSIC_NOTIFY_PLAYER");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 100, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_play_and_pause, pendingIntent);

        // TODO: 2016/7/15 上一首点击事件并发广播
        Intent intentLast = new Intent("com.example.administrator.MUSIC_NOTIFY_LAST");
        PendingIntent pendingIntentLast = PendingIntent.getBroadcast(this, 100, intentLast, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_last_music, pendingIntentLast);

        // TODO: 2016/7/15 下一首点击事件并发广播
        Intent intentNext = new Intent("com.example.administrator.MUSIC_NOTIFY_NEXT");
        PendingIntent pendingIntentNext = PendingIntent.getBroadcast(this, 100, intentNext, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.notification_next_music, pendingIntentNext);

        // TODO: 2016/7/15 退出程序
        Intent intentExit = new Intent("com.example.administrator.MUSIC_NOTIFY_EXIT");
        PendingIntent pendingIntentExit = PendingIntent.getBroadcast(this, 100, intentExit, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.music_edit, pendingIntentExit);

        // TODO: 2016/7/15 更新图标
        if (flag)
            if (mMediaPlayer.isPlaying()) {
                remoteViews.setImageViewResource(R.id.notification_play_and_pause, R.drawable.cwc);
            } else {
                remoteViews.setImageViewResource(R.id.notification_play_and_pause, R.drawable.cwa);
            }
        remoteViews.setTextViewText(R.id.notification_music_name, mMusic.getTitle());// TODO: 2016/7/15 更新音乐名
        remoteViews.setTextViewText(R.id.notification_singer_name, mMusic.getArtist());// TODO: 2016/7/15 更新歌手
        Bitmap mMusicImg = BitmapFactory.decodeFile(mMediaLists.get(current).getImage());// TODO: 2016/7/15 更新图片
        remoteViews.setImageViewBitmap(R.id.notification_img, mMusicImg == null ? BitmapFactory.decodeResource(getResources(),
                R.drawable.skin_kg_playing_bar_default_avatar) : mMusicImg);

        Notification notification = builder.build();
        mNotificationManager.notify(11, notification);
    }

    // TODO: 2016/7/15 注册广播（实现接收通知发出的广播）
    public void registerReceiver() {
        IntentFilter intentPlay = new IntentFilter("com.example.administrator.MUSIC_NOTIFY_PLAYER");
        registerReceiver(musicReceiver, intentPlay);

        IntentFilter intentLast = new IntentFilter("com.example.administrator.MUSIC_NOTIFY_LAST");
        registerReceiver(musicReceiver, intentLast);

        IntentFilter intentNext = new IntentFilter("com.example.administrator.MUSIC_NOTIFY_NEXT");
        registerReceiver(musicReceiver, intentNext);

        IntentFilter intentExit = new IntentFilter("com.example.administrator.MUSIC_NOTIFY_EXIT");
        registerReceiver(musicReceiver, intentExit);
    }

    // TODO: 2016/7/5 退出时保存当前播放信息
    public void setShared() {
        SharedPreferences.Editor editor = getSharedPreferences("music_data", MODE_PRIVATE).edit();
        editor.putInt("current", current);   // TODO: 2016/7/6  保存当前音乐的队列
        editor.putInt("mCurrentPosition", mMediaPlayer.getCurrentPosition());     // TODO: 2016/7/6  保存当前音乐播放时间
        editor.putInt("PLAY_MODE", PLAY_MODE);     // TODO: 2016/7/6  保存音乐播放模式
        editor.commit();
    }
}