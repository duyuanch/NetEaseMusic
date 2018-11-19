package shellhub.github.neteasemusic.model.impl;

import android.content.Context;
import android.content.Intent;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.LogUtils;
import com.blankj.utilcode.util.SPUtils;
import com.blankj.utilcode.util.Utils;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;

import shellhub.github.neteasemusic.model.SearchModel;
import shellhub.github.neteasemusic.model.entities.SearchHistory;
import shellhub.github.neteasemusic.model.entities.dao.SearchHistoryDao;
import shellhub.github.neteasemusic.networking.NetEaseMusicService;
import shellhub.github.neteasemusic.response.search.SearchResponse;
import shellhub.github.neteasemusic.response.search.SongsItem;
import shellhub.github.neteasemusic.response.search.artist.ArtistResponse;
import shellhub.github.neteasemusic.response.search.hot.HotResponse;
import shellhub.github.neteasemusic.response.search.mp3.SongResponse;
import shellhub.github.neteasemusic.response.search.song.detail.SongDetailResponse;
import shellhub.github.neteasemusic.response.search.video.VideoResponse;
import shellhub.github.neteasemusic.util.ConstantUtils;
import shellhub.github.neteasemusic.util.MusicUtils;
import shellhub.github.neteasemusic.util.NetEaseMusicApp;
import shellhub.github.neteasemusic.util.TagUtils;
import shellhub.github.neteasemusic.vo.NetworkMusic;

public class SearchModelImpl implements SearchModel {
    private String TAG = TagUtils.getTag(this.getClass());
    private NetEaseMusicService mNetEaseMusicService;
    private static List<String> histories = new ArrayList<>();

    public SearchModelImpl(NetEaseMusicService netEaseMusicService) {
        this.mNetEaseMusicService = netEaseMusicService;
    }

    @Override
    public void searchHot(Callback callback) {
        mNetEaseMusicService.searchHot(new NetEaseMusicService.Callback<HotResponse>() {
            @Override
            public void onSuccess(HotResponse data) {
                LogUtils.d(TAG, data);
                callback.onHotSuccess(data);
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Override
    public void searchByKeywords(String keyword, Callback callback) {
        mNetEaseMusicService.search(keyword, new NetEaseMusicService.Callback<SearchResponse>() {

            @Override
            public void onSuccess(SearchResponse searchResponse) {
                callback.onKeywordSuccess(searchResponse);

                //store search result
                for (SongsItem songsItem : searchResponse.getResult().getSongs()) {
                    mNetEaseMusicService.getSongUrl(songsItem.getId(), new NetEaseMusicService.Callback<SongResponse>(){

                        @Override
                        public void onSuccess(SongResponse data) {
                            data.getData().get(0).getUrl();
                            NetworkMusic networkMusic = new NetworkMusic();
                            networkMusic.setId(songsItem.getId());
                            networkMusic.setUrl(data.getData().get(0).getUrl());
                            networkMusic.setName(songsItem.getName());
                            networkMusic.setArtistAndAlbum(MusicUtils.getArtistAndAlbum(songsItem));
                            EventBus.getDefault().post(networkMusic);
                            LogUtils.d(TAG, networkMusic);
                        }

                        @Override
                        public void onError(Throwable e) {

                        }
                    });
                }
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Override
    public void searchVideo(String keyword, Callback callback) {
        mNetEaseMusicService.searchVideo(keyword, new NetEaseMusicService.Callback<VideoResponse>() {
            @Override
            public void onSuccess(VideoResponse data) {
                callback.onVideoSuccess(data);
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Override
    public void searchArtist(String keyword, Callback callback) {
        mNetEaseMusicService.searchArtist(keyword, new NetEaseMusicService.Callback<ArtistResponse>() {
            @Override
            public void onSuccess(ArtistResponse data) {
                callback.onArtistSuccess(data);
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

    @Override
    public void loadHistory(Callback callback) {
        histories.clear();
        new Thread(() -> {
            for (SearchHistory searchHistory : NetEaseMusicApp.getDBInstance().searchHistoryDao().getAll()) {
                histories.add(searchHistory.getKeyword());
            }
            ActivityUtils.getTopActivity().runOnUiThread(() -> {
                callback.onHistory(histories);
                LogUtils.d(TAG, histories + "restore");
            });

        }).start();
    }

    @Override
    public void saveHistory(String keyword) {
//        LogUtils.d(TAG, keyword);
//        if (histories.size() != 0 && keyword.equals(histories.get(0))) {
//            return;
//        }
//
//        if (histories.contains(keyword)) {
//            histories.remove(keyword);
//        }
//        histories.add(0, keyword);
//        //just store top 5 search history
//        if (histories.size() > 5) {
//            histories.remove(histories.size() - 1);
//        }
//
//        LogUtils.d(TAG, histories);

//        histories.remove(keyword);
//        histories.add(0, keyword);
//
//        if (histories.size() > 5) { //just store top 5
//            histories = histories.subList(0, 5);
//        }


        //save history to db
        new Thread(() -> {

            histories.clear();
            for (SearchHistory searchHistory : NetEaseMusicApp.getDBInstance().searchHistoryDao().getAll()) {
                histories.add(searchHistory.getKeyword());
            }

            if (histories.contains(keyword)) {
                histories.remove(keyword);
                histories.add(0, keyword);
            }else {
                histories.add(0, keyword);
            }

            if (histories.size() > 5) {
                histories = histories.subList(5, histories.size());
            }

            //delete all database
            NetEaseMusicApp.getDBInstance().searchHistoryDao().deleteAll();

            LogUtils.d(TAG, histories + "store");
            SearchHistoryDao searchHistoryDao = NetEaseMusicApp.getDBInstance().searchHistoryDao();
            for (int i = 0; i < histories.size() ; i++) {
                SearchHistory searchHistory = new SearchHistory(i, histories.get(i));
                searchHistoryDao.insertAll(searchHistory);
            }
        }).start();
    }

    @Override
    public void saveSong(SongsItem songsItem, Callback callback) {
        MusicUtils.saveSongId(songsItem.getId());
        SPUtils.getInstance(ConstantUtils.SP_NET_EASE_MUSIC_STATUS, Context.MODE_PRIVATE).put(ConstantUtils.SP_CURRENT_SONG_NAME_KEY, songsItem.getName());
        SPUtils.getInstance(ConstantUtils.SP_NET_EASE_MUSIC_STATUS, Context.MODE_PRIVATE).put(ConstantUtils.SP_CURRENT_SONG_ARTIST_AND_ALBUM_KEY, MusicUtils.getArtistAndAlbum(songsItem));

        mNetEaseMusicService.getSongUrl(songsItem.getId(), new NetEaseMusicService.Callback<SongResponse>(){

            @Override
            public void onSuccess(SongResponse data) {
                callback.onSongReady(data.getData().get(0).getUrl());
            }

            @Override
            public void onError(Throwable e) {

            }
        });
        mNetEaseMusicService.getSongDetail(songsItem.getId(), new NetEaseMusicService.Callback<SongDetailResponse>(){
            @Override
            public void onSuccess(SongDetailResponse data) {
                //store pic
                LogUtils.d(TAG, data.getSongs().get(0).getAl().getPicUrl());
                SPUtils.getInstance(ConstantUtils.SP_NET_EASE_MUSIC_STATUS, Context.MODE_PRIVATE)
                        .put(ConstantUtils.SP_CURRENT_SONG_ALBUM_URL_KEY, data.getSongs().get(0).getAl().getPicUrl());
                Utils.getApp().sendBroadcast(new Intent(ConstantUtils.ACTION_UPDATE_NOTIFICATION));
            }

            @Override
            public void onError(Throwable e) {

            }
        });
    }

}
