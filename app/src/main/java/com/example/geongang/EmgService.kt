package com.example.geongang

import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST

public interface EmgService {
    @POST("/emg")
    fun getEmg(@Body EmgRequest: EmgRequest): Observable<Emg>
}