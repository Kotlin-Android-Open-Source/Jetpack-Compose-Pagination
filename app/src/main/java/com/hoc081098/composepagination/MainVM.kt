package com.hoc081098.composepagination

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hoc081098.composepagination.PlaceholderState.Failure
import com.hoc081098.composepagination.PlaceholderState.Idle
import com.hoc081098.composepagination.PlaceholderState.Loading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PlaceholderState {
  data class Idle(val isEmpty: Boolean) : PlaceholderState()
  object Loading : PlaceholderState()
  data class Failure(val throwable: Throwable) : PlaceholderState()
}

abstract class MainVM : ViewModel() {
  abstract val usersStateFlow: StateFlow<List<User>>

  abstract val firstPageStateFlow: StateFlow<PlaceholderState>

  abstract val loadingStateFlow: StateFlow<PlaceholderState>

  abstract val isRefreshingStateFlow: StateFlow<Boolean>

  @MainThread
  abstract fun loadNextPage()

  @MainThread
  abstract fun retry()

  @MainThread
  abstract fun refresh()

  class Factory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if (modelClass == MainVM::class.java) {
        return MainVMImpl(::getUsers) as T
      }
      error("Unknown modelClass: $modelClass")
    }
  }
}

private class MainVMImpl(
  private val getUsers: suspend (start: Int, limit: Int) -> List<User>
) : MainVM() {

  //region Private fields

  private val _usersSF = MutableStateFlow(emptyList<User>())
  private val _loadingStateSF = MutableStateFlow<PlaceholderState>(Idle(true))
  private val _firstPageStateSF = MutableStateFlow<PlaceholderState>(Idle(true))
  private val _isRefreshingSF = MutableStateFlow(false)

  private var isFirstPage = true
  private var loadedAllPage = false

  private inline val shouldLoadNextPage: Boolean
    get() = if (isFirstPage) {
      _firstPageStateSF.value is Idle
    } else {
      _loadingStateSF.value is Idle
    } && !loadedAllPage

  private inline val shouldRetry: Boolean
    get() = if (isFirstPage) {
      _firstPageStateSF.value is Failure
    } else {
      _loadingStateSF.value is Failure
    }

  //endregion

  init {
    loadNextPage()
  }

  //region Public

  override val usersStateFlow get() = _usersSF.asStateFlow()

  override val firstPageStateFlow get() = _firstPageStateSF.asStateFlow()

  override val loadingStateFlow get() = _loadingStateSF.asStateFlow()

  override val isRefreshingStateFlow get() = _isRefreshingSF.asStateFlow()

  @MainThread
  override fun loadNextPage() {
    if (shouldLoadNextPage) {
      loadPageInternal()
    }
  }

  @MainThread
  override fun retry() {
    if (shouldRetry) {
      loadPageInternal()
    }
  }

  @MainThread
  override fun refresh() {
    loadPageInternal(refresh = true)
  }
  //endregion

  //region Private methods

  @MainThread
  private fun updateState(state: PlaceholderState) {
    if (isFirstPage) {
      _firstPageStateSF.value = state
    } else {
      _loadingStateSF.value = state
    }
  }

  private fun loadPageInternal(refresh: Boolean = false) {
    viewModelScope.launch {
      if (refresh) {
        _isRefreshingSF.value = true
      } else {
        updateState(Loading)
      }

      val currentList = if (refresh) emptyList() else _usersSF.value

      runCatching { getUsers(currentList.size, LIMIT) }
        .fold(
          onSuccess = {
            if (refresh) {
              _isRefreshingSF.value = false
            } else {
              updateState(Idle(it.isEmpty()))
            }
            _usersSF.value = currentList + it

            isFirstPage = false
            loadedAllPage = it.isEmpty()
          },
          onFailure = {
            if (refresh) {
              _isRefreshingSF.value = false
            } else {
              updateState(Failure(it))
            }
          }
        )
    }
  }

  //endregion

  companion object {
    private const val LIMIT = 20
  }
}