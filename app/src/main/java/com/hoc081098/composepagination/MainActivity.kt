package com.hoc081098.composepagination

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hoc081098.composepagination.ui.theme.ComposePaginationTheme
import java.io.IOException

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      ComposePaginationTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          Scaffold(
            topBar = {
              TopAppBar(
                title = {
                  Text(text = "Jetpack Compose Pagination")
                }
              )
            }
          ) {
            MainScreen()
          }
        }
      }
    }
  }
}


// TODO: Add refreshing
@Composable
fun MainScreen() {
  val vm = viewModel<MainVM>(factory = MainVM.Factory())

  when (val firstPageState = vm.firstPageStateFlow.collectAsState().value) {
    PlaceholderState.Loading -> ItemLoading(
      modifier = Modifier.fillMaxSize(),
    )
    is PlaceholderState.Failure -> FailureItem(
      throwable = firstPageState.throwable,
      onRetry = vm::retry,
      modifier = Modifier.fillMaxSize(),
    )
    is PlaceholderState.Idle -> {
      if (firstPageState.isEmpty) {
         return Column(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Text(
            "Empty list",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      val userList by vm.usersStateFlow.collectAsState()
      val loadingState by vm.loadingStateFlow.collectAsState()

      UserList(
        state = loadingState,
        users = userList,
        onRetry = vm::retry,
        loadNextPage = vm::loadNextPage,
      )
    }
  }
}

@Composable
fun UserList(
  state: PlaceholderState,
  users: List<User>,
  onRetry: () -> Unit,
  loadNextPage: () -> Unit,
) {
  val threshold = 3
  val lastIndex = users.lastIndex

  LazyColumn(
    verticalArrangement = Arrangement.spacedBy(12.dp),
    contentPadding = PaddingValues(all = 8.dp),
  ) {
    items(
      count = users.size + 1,
      key = { users.getOrNull(it)?.uid ?: "PLACEHOLDER" },
    ) { index ->
      val user = users.getOrNull(index)
      val parentMaxWidth = Modifier.fillParentMaxWidth()

      if (user != null) {
        if (index + threshold >= lastIndex) {
          SideEffect {
            Log.d("###", "$index $lastIndex")
            loadNextPage()
          }
        }

        Column(
          modifier = parentMaxWidth,
        ) {
          Text(
            text = user.name,
            style = MaterialTheme.typography.subtitle1,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = user.email,
            style = MaterialTheme.typography.caption,
          )
        }

        return@items
      }

      when (state) {
        is PlaceholderState.Failure -> FailureItem(
          throwable = state.throwable,
          onRetry = onRetry,
          modifier = parentMaxWidth,
        )
        is PlaceholderState.Idle -> if (!state.isEmpty) {
          Spacer(modifier = parentMaxWidth.requiredHeight(48.dp))
        }
        PlaceholderState.Loading -> ItemLoading(modifier = parentMaxWidth)
      }
    }
  }
}

@Composable
fun ItemLoading(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    CircularProgressIndicator()
  }
}

@Composable
fun FailureItem(
  throwable: Throwable,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      throwable.message ?: "Unknown error",
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRetry) {
      Text(text = "RETRY")
    }
  }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  ComposePaginationTheme {
    FailureItem(
      throwable = IOException("Error network"),
      onRetry = {},
      modifier = Modifier.fillMaxHeight(),
    )
  }
}
