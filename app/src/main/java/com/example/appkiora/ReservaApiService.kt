package com.example.appkiora

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Comunicação entre o aplicativo Android
 * e a API de reservas do restaurante Kiora.
 *
 * Permite operações de visitantes sem login
 * e clientes autenticados.
 */
interface ReservaApiService {

    /**
     * Abre ou recupera a sessão do visitante.
     *
     * Retorna:
     * - Chave de visitante
     * - Token CSRF
     * - Data atual do restaurante
     * - Última data permitida para reserva
     */
    @GET("api/reservas/sessao")
    suspend fun abrirSessao():
            Response<SessaoReservaApiResponse>

    /**
     * Consulta os horários de reserva.
     */
    @GET("api/reservas/horarios")
    suspend fun consultarHorarios(
        @Query("data") data: String,
        @Query("pessoas") pessoas: Int
    ): Response<List<HorarioReservaApiResponse>>

    /**
     * Envia uma nova solicitação de reserva.
     *
     * A autenticação não é obrigatória.
     */
    @POST("api/reservas")
    suspend fun criarReserva(
        @Body request: CriarReservaApiRequest
    ): Response<ReservaApiResponse>

    /**
     * Consulta as reservas associadas
     * ao visitante ou usuário autenticado.
     *
     * O servidor retorna diretamente uma lista.
     */
    @GET("api/reservas/minhas")
    suspend fun listarMinhasReservas():
            Response<List<ReservaApiResponse>>

    /**
     * Consulta uma reserva específica.
     *
     * O código é um UUID fornecido pelo servidor.
     */
    @GET("api/reservas/{codigo}")
    suspend fun consultarReserva(
        @Path("codigo") codigo: String
    ): Response<ReservaApiResponse>

    /**
     * Solicita o cancelamento de uma reserva.
     *
     * O servidor verifica o código, a versão,
     * a identidade do solicitante e a situação.
     */
    @POST("api/reservas/cancelar")
    suspend fun cancelarReserva(
        @Body request: CancelarReservaApiRequest
    ): Response<CancelarReservaApiResponse>
}

/**
 * Dados de identificação da sessão.
 */
data class SessaoReservaApiResponse(
    val chaveVisitante: String,
    val csrfToken: String,
    val hoje: String,
    val ultimaData: String,
    val fuso: String
)

/**
 * Horários retornados pelo restaurante.
 */
data class HorarioReservaApiResponse(
    val horario: String,
    val encerramento: String,
    val minutos: Int,
    val disponivel: Boolean
)

/**
 * Dados necessários para cancelar uma reserva.
 *
 * Tanto codigo quanto versao são UUIDs.
 */
data class CancelarReservaApiRequest(
    val codigo: String,
    val versao: String
)

/**
 * Resposta de cancelamento da API.
 *
 * O backend retorna:
 * {"sucesso":true}
 */
data class CancelarReservaApiResponse(
    val sucesso: Boolean
)