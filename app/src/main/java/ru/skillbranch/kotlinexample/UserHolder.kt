package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.lang.IllegalArgumentException

object UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this email already exists")
                map[user.login] = user
            }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        return User.makeUser(fullName, phone = rawPhone)
            .also { user ->
                if (map.containsKey(user.login))
                    throw IllegalArgumentException("A user with this phone already exists")
                map[user.login] = user
            }
    }

    fun loginUser(login: String, password: String): String? {
        return map[correctLogin(login)]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    private fun correctLogin(login: String): String {
        return login.trim().let {
            if (it.matches("^\\+\\d.*".toRegex()))
                it.replace("[^+\\d]".toRegex(), "")
            else it
        }
    }

    fun requestAccessCode(login: String) {
        map[correctLogin(login)]?.updateAccessCode()
    }

    fun importUsers(list: List<String>): List<User> = list.map { str ->
        val fields = str.split(";").map { it.trim() }
        val fullName = fields[0].fullNameToPair()
        val firstName = fullName.first
        val lastName = fullName.second
        val email = fields[1]
        val salt = fields[2].substringBefore(":")
        val hash = fields[2].substringAfter(":")
        val phone = fields[3]
        return@map User(
            firstName,
            lastName?.let { if (it.isBlank()) null else it },
            email.let { if (it.isBlank()) null else it },
            salt.let { if (it.isBlank()) null else it },
            hash.let { if (it.isBlank()) null else it },
            phone.let { if (it.isBlank()) null else it }
        )
    }

    private fun String.fullNameToPair(): Pair<String, String?> {
        return split(" ").filter { it.isNotBlank() }.run {
            when (size) {
                1 -> first() to null
                2 -> first() to last()
                else -> throw IllegalArgumentException(
                    "Fullname must contain only first name and last name, current split result ${this@fullNameToPair}"
                )
            }
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun clearHolder() {
        map.clear()
    }
}