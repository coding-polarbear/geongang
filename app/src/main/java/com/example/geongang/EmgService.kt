package com.example.geongang

import com.example.geongang.entity.Emg
import com.example.geongang.entity.EmgRequest
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

public interface EmgService {
    @POST("/emg")
    fun getEmg(@Body EmgRequest: EmgRequest): Observable<Emg>
}