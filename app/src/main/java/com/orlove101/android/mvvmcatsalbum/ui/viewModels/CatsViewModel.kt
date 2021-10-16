package com.orlove101.android.mvvmcatsalbum.ui.viewModels

import android.util.Log
import androidx.lifecycle.*
import com.orlove101.android.mvvmcatsalbum.data.models.CatsResponse
import com.orlove101.android.mvvmcatsalbum.repository.CatsRepository
import com.orlove101.android.mvvmcatsalbum.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class CatsViewModel @Inject constructor(
    val catsRepository: CatsRepository
) : ViewModel() {
    val cats: MutableLiveData<Resource<CatsResponse>> = MutableLiveData()
    var catsPage = 1
    var catsResponse: CatsResponse? = null

    init {
        getCats()
    }

    fun getCats() = viewModelScope.launch {
        cats.postValue(Resource.Loading())
        val response = catsRepository.getCats(catsPage)
        cats.postValue(handleCatsResponse(response))
    }

    private fun handleCatsResponse(response: Response<CatsResponse>) : Resource<CatsResponse> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->

                Log.d(TAG, resultResponse.toString())

                catsPage++
                if(catsResponse == null) {
                    catsResponse = resultResponse
                } else {
                    val oldCats = catsResponse
                    val newCats = resultResponse
                    oldCats?.addAll(newCats)
                }
                return Resource.Success(catsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }
}

private const val TAG = "NewsViewModel"
