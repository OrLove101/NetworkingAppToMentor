package com.orlove101.android.mvvmcatsalbum.repository

import com.orlove101.android.mvvmcatsalbum.api.CatsAPI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CatsRepository @Inject constructor(
    val api: CatsAPI
) {
    suspend fun getCats(pageNumber: Int) =
        api.getCats(pageNumber = pageNumber)
}
