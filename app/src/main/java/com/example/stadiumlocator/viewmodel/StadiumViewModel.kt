package com.example.stadiumlocator.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.stadiumlocator.model.Stadium
import com.example.stadiumlocator.repository.StadiumRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StadiumViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StadiumRepository(application)

    private val _stadiums = MutableLiveData<List<Stadium>>()
    val stadiums: LiveData<List<Stadium>> get() = _stadiums

    private val _feedback = MutableLiveData<Pair<String, Boolean>>()
    val feedback: LiveData<Pair<String, Boolean>> get() = _feedback

    fun getStadiums() {
        CoroutineScope(Dispatchers.Main).launch {
            when (val result = repository.getNearbyStadiums()) {
                is StadiumRepository.Result.Success -> {
                    _stadiums.value = result.stadiums
                }
                is StadiumRepository.Result.Error -> {
                    _feedback.value = Pair(result.message, false)
                }
            }
        }
    }

    fun deleteStadium(stadium: Stadium) {
        CoroutineScope(Dispatchers.Main).launch {
            when (val result = repository.deleteStadium(stadium)) {
                is StadiumRepository.Result.Success -> {
                    _stadiums.value = _stadiums.value?.filter { it != stadium }
                }
                is StadiumRepository.Result.Error -> {
                    _feedback.value = Pair(result.message, false)
                }
            }
        }
    }

    fun addStadium(stadiumName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            when (val result = repository.addStadium(stadiumName)) {
                is StadiumRepository.StadiumResult.Add -> {
                    _stadiums.value = _stadiums.value?.plus(result.stadium)?.sortedBy { it.distance }
                    _feedback.value = Pair("Stadio aggiunto!", true)
                }
                is StadiumRepository.StadiumResult.Error -> {
                    _feedback.value = Pair(result.message, false)
                }
            }
        }
    }
}
