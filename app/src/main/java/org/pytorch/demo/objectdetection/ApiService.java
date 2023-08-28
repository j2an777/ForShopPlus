package org.pytorch.demo.objectdetection;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {
    @Multipart
    @POST("test1") // 고화질 전용
    Call<ResponseBody> test1( //이미지 한 개 보내기
                              @Part MultipartBody.Part image
    );
    @Multipart
    @POST("test0") //객체제거 전용
    Call<ResponseBody> test0(
            @Part MultipartBody.Part image
    );
    @POST("test2") // Flask 서버의 엔드포인트 //객체제거전용
    Call<ResponseBody> test2();

    @POST("test3") // Flask 서버의 엔드포인트//보내는거 없이 받는 용도 //고화질전용
    Call<ResponseBody> test3();

}