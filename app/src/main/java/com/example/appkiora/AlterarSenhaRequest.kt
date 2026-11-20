
package com.example.appkiora

/**
 * Dados enviados para a API ao alterar a senha.
 */
data class AlterarSenhaRequest(
    val senhaAtual: String,
    val novaSenha: String,
    val confirmarNovaSenha: String
)
