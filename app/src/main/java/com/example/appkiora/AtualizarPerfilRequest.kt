
package com.example.appkiora

/**
 * Representa os dados enviados para atualizar o perfil
 * do usuário autenticado na API do Kiora.
 */
data class AtualizarPerfilRequest(
    val nome: String,
    val email: String
)
