
package com.example.appkiora

/**
 * Representa respostas simples da API do Kiora.
 */
data class MensagemResponse(
    val sucesso: Boolean = false,
    val mensagem: String = ""
)
