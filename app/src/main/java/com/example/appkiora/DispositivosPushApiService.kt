package com.example.appkiora

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Comunicação entre o Kiora Mobile e a API responsável pelo registro de dispositivos Firebase Cloud Messaging (FCM).
 */
interface DispositivosPushApiService {

    /**
     * Registra ou atualiza o token FCM.
     * POST /api/reservas/dispositivos
     *
     * A API associa o dispositivo ao visitante ou ao cliente autenticado.
     */
    @POST("api/reservas/dispositivos")
    suspend fun registrarDispositivo(
        @Body request: DispositivoPushRequest
    ): Response<DispositivoPushResponse>

    /**
     * Desativa o recebimento de notificações.
     *
     * POST /api/reservas/dispositivos/desativar
     *
     * O servidor verifica se o dispositivo está associado à identidade solicitante.
     */
    @POST("api/reservas/dispositivos/desativar")
    suspend fun desativarDispositivo(
        @Body request: DispositivoPushRequest
    ): Response<DispositivoPushResponse>
}

/**
 * Contrato enviado ao backend ASP.NET Core.
 *
 * Exemplo:
 * {
 *     "token": "TOKEN_FCM_DO_DISPOSITIVO"
 * }
 */
data class DispositivoPushRequest(
    val token: String
)

/**
 * Resposta utilizada pelos dois endpoints.
 *
 * Exemplo:
 * {
 *     "sucesso": true
 * }
 */
data class DispositivoPushResponse(
    val sucesso: Boolean
)