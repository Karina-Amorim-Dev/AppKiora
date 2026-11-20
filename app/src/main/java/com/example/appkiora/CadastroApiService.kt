
package com.example.appkiora

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Define o endpoint de cadastro da API do Kiora.
 */
interface CadastroApiService {

    @POST("api/usuarios/cadastro")
    suspend fun cadastrar(
        @Body request: CadastroRequest
    ): Response<CadastroResponse>
}
