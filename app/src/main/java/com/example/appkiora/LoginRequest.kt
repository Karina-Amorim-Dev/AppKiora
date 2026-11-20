package com.example.appkiora

/**
 * Representa os dados necessários para solicitar
 * a autenticação do usuário pela API do Kiora.
 */
data class LoginRequest(
    val email: String,
    val senha: String
)

