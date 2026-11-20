
package com.example.appkiora

import android.content.Context

/**
 * Armazena e recupera o token JWT recebido da API.
 */
class TokenManager(context: Context) {

    private val preferencias = context.getSharedPreferences(
        "kiora_autenticacao",
        Context.MODE_PRIVATE
    )

    fun salvarToken(token: String) {
        preferencias.edit()
            .putString("token_jwt", token)
            .apply()
    }

    fun obterToken(): String? {
        return preferencias.getString("token_jwt", null)
    }

    fun limparToken() {
        preferencias.edit()
            .remove("token_jwt")
            .apply()
    }
}
