package com.hoc081098.composepagination

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

//region Models
data class User(
  val uid: Int,
  val name: String,
  val email: String
)

object ApiError : Throwable(message = "Api error")
//endregion

//region Fake api calling
suspend fun getUsers(start: Int, limit: Int): List<User> {
  return withContext(Dispatchers.IO) {
    Log.d("###", "getUsers { start: $start, limit: $limit }")
    delay(3_000L)

    val page = start / limit

    // throws at page 2
    if (page == 2 && Random.nextBoolean()) {
      throw ApiError
    }

    // throws at page 0
    if (page == 0 && Random.nextBoolean()) {
      throw ApiError
    }

    // returns empty list at page 4
    if (page == 4) {
      Log.d("###", "Load done")
      emptyList()
    } else {
      List(limit) {
        User(
          uid = start + it,
          name = "Name ${start + it}",
          email = "email${start + it}@gmail.com",
        )
      }
    }
  }
}
//endregion