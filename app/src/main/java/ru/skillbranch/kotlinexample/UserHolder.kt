package ru.skillbranch.kotlinexample

import java.lang.IllegalArgumentException

class UserHolder {
    private val map = mutableMapOf<String, User>()

    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): User {
        return User.makeUser(fullName, email = email, password = password)
            .also { user -> map[user.login] = user }
    }

    fun registerUserByPhone(
        fullName: String,
        rawPhone: String
    ): User {
        if (map.containsKey(rawPhone))
            throw IllegalArgumentException("A user with this phone already exists")

        return User.makeUser(fullName, phone = rawPhone)
            .also { user -> map[user.login] = user }
    }

    fun loginUser(login: String, password: String): String? {
        return map[login.trim()]?.run {
            if (checkPassword(password)) this.userInfo
            else null
        }
    }

    fun requestAccessCode(login: String) {
        map[login]?.updateAccessCode()
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
}