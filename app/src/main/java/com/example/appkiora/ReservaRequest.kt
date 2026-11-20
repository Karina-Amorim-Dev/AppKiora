package com.example.appkiora

/**
 * Modelo original utilizado para preparar
 * os dados da tela de reservas.
 */
data class ReservaRequest(
    val data: String,
    val horario: String,
    val quantidadePessoas: Int,
    val observacoes: String? = null
)

/**
 * Dados exigidos pela API do site Kiora
 * para criar uma reserva.
 */
data class CriarReservaApiRequest(
    val chaveSolicitacao: String,
    val nome: String,
    val telefone: String,
    val pessoas: Int,
    val data: String,
    val horario: String,
    val aceitaPermanenciaReduzida: Boolean = false
)