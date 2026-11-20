package com.example.appkiora

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

/**
 * Comunicação de autenticação e perfil
 * entre o Android e a API do Kiora.
 */
interface LoginApiService {

    // Autenticação do usuário.
    @POST("api/usuarios/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    // Consulta do perfil autenticado.
    @GET("api/usuarios/perfil")
    suspend fun buscarPerfil(): Response<LoginResponse>

    // Atualização de nome e e-mail.
    @PUT("api/usuarios/perfil")
    suspend fun atualizarPerfil(
        @Body request: AtualizarPerfilRequest
    ): Response<LoginResponse>

    // Alteração de senha com retorno do novo JWT.
    @POST("api/usuarios/alterar-senha")
    suspend fun alterarSenha(
        @Body request: AlterarSenhaRequest
    ): Response<LoginResponse>
}