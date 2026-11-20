package com.example.appkiora

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Comunicação com a API de recuperação
 * de senha do Kiora Restaurante.
 *
 * A recuperação não exige login.
 */
interface RecuperacaoSenhaApiService {

    /**
     * Solicita o envio do código ao e-mail.
     */
    @POST("api/usuarios/recuperacao/solicitar")
    suspend fun solicitarCodigo(
        @Body request: SolicitarCodigoRecuperacaoRequest
    ): Response<RecuperacaoSenhaApiResponse>

    /**
     * Valida o código de 6 dígitos
     * recebido por e-mail.
     */
    @POST("api/usuarios/recuperacao/validar")
    suspend fun validarCodigo(
        @Body request: ValidarCodigoRecuperacaoRequest
    ): Response<RecuperacaoSenhaApiResponse>

    /**
     * Define a nova senha após
     * validar o código de recuperação.
     */
    @POST("api/usuarios/recuperacao/redefinir")
    suspend fun redefinirSenha(
        @Body request: RedefinirSenhaRecuperacaoRequest
    ): Response<RecuperacaoSenhaApiResponse>
}

/**
 * Solicitação do código de recuperação.
 */
data class SolicitarCodigoRecuperacaoRequest(
    val email: String
)

/**
 * Validação do código recebido.
 */
data class ValidarCodigoRecuperacaoRequest(
    val email: String,
    val codigo: String
)

/**
 * Redefinição da senha.
 */
data class RedefinirSenhaRecuperacaoRequest(
    val email: String,
    val codigo: String,
    val novaSenha: String,
    val confirmarNovaSenha: String
)

/**
 * Resposta esperada da API.
 */
data class RecuperacaoSenhaApiResponse(
    val sucesso: Boolean,
    val mensagem: String
)