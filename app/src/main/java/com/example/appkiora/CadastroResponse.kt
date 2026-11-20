package com.example.appkiora

/**
 * Representa a resposta que a API do Kiora poderá retornar
 * após uma solicitação de cadastro.
 */
data class CadastroResponse(

    // Indica se a operação foi concluída com sucesso.
    val sucesso: Boolean,

    // Mensagem que poderá ser exibida ao usuário.
    val mensagem: String,

    // Código opcional para identificar o resultado.
    // Exemplos: EMAIL_EXISTENTE ou DADOS_INVALIDOS.
    val codigo: String? = null
)

