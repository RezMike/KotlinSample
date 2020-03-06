package ru.skillbranch.kotlinexample

import androidx.annotation.VisibleForTesting
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom

class User private constructor(
    private val firstName: String,
    private val lastName: String?,
    email: String? = null,
    rawPhone: String? = null,
    meta: Map<String, Any>? = null
) {
    val userInfo: String

    private val fullName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .capitalize()

    private val initials: String
        get() = listOfNotNull(firstName, lastName)
            .map { it.first().toUpperCase() }
            .joinToString(" ")

    private var phone: String? = null

    private var _login: String? = null
    var login: String
        get() = _login!!
        set(value) {
            _login = value.toLowerCase()
        }

    private var salt: String? = null

    private lateinit var passwordHash: String

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    var accessCode: String? = null

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        password: String
    ) : this(firstName, lastName, email = email, meta = mapOf("auth" to "password")) {
        passwordHash = encrypt(password)
    }

    constructor(
        firstName: String,
        lastName: String?,
        phone: String?
    ) : this(firstName, lastName, rawPhone = phone, meta = mapOf("auth" to "sms")) {
        updateAccessCode()
    }

    constructor(
        firstName: String,
        lastName: String?,
        email: String?,
        phone: String?,
        salt: String?,
        hash: String?
    ) : this(firstName, lastName, email, phone, mapOf("src" to "csv")) {
        this.salt = salt
        this.passwordHash = hash ?: "null"
    }

    init {
        if (salt == null)
            salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.toString()
        check(!firstName.isBlank()) { "FirstName must be not blank" }
        check(!email.isNullOrBlank() || rawPhone.isNullOrBlank()) { "Email or phone must be not blank" }

        if (rawPhone?.contains("[^+\\d]".toRegex()) == true)
            throw IllegalArgumentException("Enter a valid phone number starting with a + and containing 11 digits")

        phone = rawPhone
        login = email ?: phone!!

        userInfo = """
            firstName: $firstName
            lastName: $lastName
            login: $login
            fullName: $fullName
            initials: $initials
            email: $email
            phone: $phone
            meta: $meta
        """.trimIndent()
    }

    fun updateAccessCode() {
        val code = generateAccessCode()
        passwordHash = encrypt(code)
        accessCode = code
        sendAccessCodeToUser(phone, code)
    }

    fun checkPassword(pass: String) = encrypt(pass) == passwordHash

    fun changePassword(oldPass: String, newPass: String) {
        if (checkPassword(oldPass)) passwordHash = encrypt(newPass)
        else throw IllegalArgumentException("The entered password does not match the current password")
    }

    private fun encrypt(password: String) = (salt + password).md5()

    private fun generateAccessCode(): String {
        val possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return StringBuilder().apply {
            repeat(6) {
                val index = possible.indices.random()
                append(possible[index])
            }
        }.toString()
    }

    private fun sendAccessCodeToUser(phone: String?, code: String) {
        println("...sending...")
    }

    private fun String.md5(): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(toByteArray())
        val hexString = BigInteger(1, digest).toString(16)
        return hexString.padStart(32, '0')
    }

    companion object Factory {

        fun makeUser(
            fullName: String,
            email: String? = null,
            password: String? = null,
            phone: String? = null
        ): User {
            val (firstName, lastName) = fullName.fullNameToPair()

            return when {
                !phone.isNullOrBlank() -> User(firstName, lastName, phone)
                !email.isNullOrBlank() && !password.isNullOrBlank() ->
                    User(firstName, lastName, email, password)
                else -> throw IllegalArgumentException("Email or phone must be not null or blank")
            }
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
}
