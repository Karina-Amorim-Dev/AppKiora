package com.example.appkiora

/**
 * Modelo original do aplicativo.
 */
data class ReservaResponse(
    val sucesso: Boolean,
    val mensagem: String,
    val idReserva: Int? = null
)

/**
 * Resposta retornada pelo servidor Kiora
 * após criar ou consultar uma reserva.
 */
data class ReservaApiResponse(
    val codigo: String,
    val nome: String,
    val pessoas: Int,
    val tipo: Int,
    val situacao: Int,
    val situacaoTexto: String,
    val inicioUtc: String?,
    val fimUtc: String?,
    val expiraEmUtc: String,
    val posicaoFila: Int?,
    val mesa: String?,
    val podeCancelar: Boolean,
    val versao: String
)