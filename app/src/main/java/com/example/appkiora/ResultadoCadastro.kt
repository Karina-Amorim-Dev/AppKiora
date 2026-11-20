package com.example.appkiora

/**
 * Representa os possíveis resultados de uma tentativa
 * de cadastro no Kiora.
 */
sealed class ResultadoCadastro {

    // O cadastro foi realizado com sucesso.
    data class Sucesso(
        val mensagem: String
    ) : ResultadoCadastro()

    // O e-mail informado já está cadastrado.
    data class EmailExistente(
        val mensagem: String
    ) : ResultadoCadastro()

    // Os dados enviados não passaram pela validação.
    data class DadosInvalidos(
        val mensagem: String
    ) : ResultadoCadastro()

    // O aplicativo não conseguiu se comunicar com a API.
    data class ErroConexao(
        val mensagem: String
    ) : ResultadoCadastro()

    // Ocorreu um erro inesperado.
    data class ErroGeral(
        val mensagem: String
    ) : ResultadoCadastro()
}

