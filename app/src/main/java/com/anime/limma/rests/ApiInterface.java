package com.anime.limma.rests;

import com.anime.limma.callbacks.CallbackAds;
import com.anime.limma.callbacks.CallbackCategories;
import com.anime.limma.callbacks.CallbackCategoryDetails;
import com.anime.limma.callbacks.CallbackListVideo;
import com.anime.limma.callbacks.CallbackSettings;
import com.anime.limma.callbacks.CallbackUser;
import com.anime.limma.callbacks.CallbackVideoDetail;
import com.anime.limma.models.Settings;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Query;

public interface ApiInterface {

    String CACHE = "Cache-Control: max-age=0";
    String AGENT = "Data-Agent: Your Videos Channel";

    @Headers({CACHE, AGENT})
    @GET("api/get_videos")
    Call<CallbackListVideo> getVideos(
            @Query("page") int page,
            @Query("count") int count,
            @Query("sort") String sort,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_post_detail")
    Call<CallbackVideoDetail> getVideoDetail(
            @Query("id") String id
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_category_index")
    Call<CallbackCategories> getAllCategories(
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_category_videos")
    Call<CallbackCategoryDetails> getCategoryVideos(
            @Query("id") int id,
            @Query("page") int page,
            @Query("count") int count,
            @Query("sort") String sort,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_search_results")
    Call<CallbackListVideo> getSearchPosts(
            @Query("search") String search,
            @Query("count") int count,
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_ads")
    Call<CallbackAds> getAds(
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_settings")
    Call<CallbackSettings> getSettings(
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_privacy_policy")
    Call<Settings> getPrivacyPolicy(
            @Query("api_key") String api_key
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_user_token")
    Call<CallbackUser> getUserToken(
            @Query("user_unique_id") String user_unique_id
    );

    @Headers({CACHE, AGENT})
    @GET("api/get_package_name")
    Call<Settings> getPackageName();

}
