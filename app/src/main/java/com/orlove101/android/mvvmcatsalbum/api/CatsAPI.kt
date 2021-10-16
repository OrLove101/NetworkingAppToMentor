package com.orlove101.android.mvvmcatsalbum.api

import com.orlove101.android.mvvmcatsalbum.data.models.CatsResponse
import com.orlove101.android.mvvmcatsalbum.util.API_KEY_CATS
import com.orlove101.android.mvvmcatsalbum.util.QUERY_PAGE_SIZE
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CatsAPI {
    @GET("v1/images/search")
    suspend fun getCats(
        @Query("api_key")
        apiKey: String = API_KEY_CATS,
        @Query("limit")
        pagesQuantity: Int = QUERY_PAGE_SIZE,
        @Query("page")
        pageNumber: Int = 1,
        @Query("order")
        queryOrder: String = "DESC",
    ): Response<CatsResponse>
}
