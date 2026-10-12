package com.example.myapplication.feature.account.domain.model

data class AccountSession(
    val accountId: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)
