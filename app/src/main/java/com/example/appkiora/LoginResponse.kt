package com.example.appkiora

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

/**
 * Representa a resposta devolvida pela API no login.
 */
data class LoginResponse(
    val sucesso: Boolean,
    val mensagem: String,

    // O token será preenchido quando o login for válido.
    val token: String? = null,

    @SerializedName("tipoToken")
    val tipoToken: String? = null,

    // Mantemos os dados do usuário em JSON até confirmar seus campos.
    val usuario: JsonObject? = null
)