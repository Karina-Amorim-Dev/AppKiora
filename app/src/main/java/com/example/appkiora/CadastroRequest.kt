package com.example.appkiora

/**
 * Representa os dados enviados pelo aplicativo
 * para cadastrar um cliente na API do Kiora.
 */
data class CadastroRequest(

    // Nome completo informado pelo cliente.
    val nome: String,

    // E-mail utilizado para identificar a conta.
    val email: String,

    // Senha informada pelo cliente.
    val senha: String,

    // Confirmação da senha exigida pelo servidor.
    val confirmarSenha: String,

    // Aceite obrigatório dos termos e da política de privacidade.
    val aceiteTermos: Boolean
)
