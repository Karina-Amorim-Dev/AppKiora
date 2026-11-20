package com.example.appkiora

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Responsável pela comunicação de autenticação
 * entre o aplicativo Android e a API do Kiora.
 */
class LoginRepository {

    private val gson = Gson()

    /**
     * Realiza o login do usuário na API.
     */
    suspend fun realizarLogin(
        email: String,
        senha: String
    ): LoginResponse = withContext(Dispatchers.IO) {

        val request = LoginRequest(
            email = email.trim(),
            senha = senha
        )

        val retrofit = RetrofitClient.instancia

        // Diagnóstico da URL utilizada pelo aplicativo.
        val urlLogin = retrofit.baseUrl()
            .resolve("api/usuarios/login")

        Log.d(
            "KIORA_URL",
            "URL BASE: ${retrofit.baseUrl()}"
        )

        Log.d(
            "KIORA_URL",
            "ENDPOINT LOGIN: POST $urlLogin"
        )

        val api = retrofit.create(
            LoginApiService::class.java
        )

        // Executa a requisição HTTP.
        val resposta = try {

            api.login(request)

        } catch (erro: IOException) {

            Log.e(
                "KIORA_LOGIN",
                "Falha de conexão: " +
                        erro.javaClass.simpleName
            )

            throw IOException(
                "Não foi possível conectar ao servidor Kiora.",
                erro
            )
        }

        Log.d(
            "KIORA_LOGIN",
            "Resposta da API: HTTP ${resposta.code()}"
        )

        if (resposta.isSuccessful) {

            val dados = resposta.body()
                ?: throw Exception(
                    "A API respondeu sem os dados do login."
                )

            if (!dados.sucesso) {
                throw Exception(
                    dados.mensagem.ifBlank {
                        "O servidor não autorizou o login."
                    }
                )
            }

            if (dados.token.isNullOrBlank()) {
                throw Exception(
                    "O servidor não retornou o token JWT."
                )
            }

            Log.d(
                "KIORA_LOGIN",
                "Login confirmado pela API."
            )

            return@withContext dados
        }

        val mensagem = extrairMensagemErro(
            resposta.errorBody()?.string()
        ) ?: when (resposta.code()) {

            400 -> "Dados de login inválidos."

            401 -> "E-mail ou senha não reconhecidos."

            403 -> "Acesso não autorizado."

            404 -> "Rota de login não encontrada."

            429 -> "Muitas tentativas de login."

            500 -> "Erro interno no servidor Kiora."

            502 -> "Falha na comunicação com o servidor."

            503 -> "Servidor temporariamente indisponível."

            else -> "Não foi possível realizar o login."
        }

        throw Exception(
            "$mensagem (HTTP ${resposta.code()})"
        )
    }

    /**
     * Consulta o perfil do usuário autenticado.
     *
     * O Retrofit envia o JWT automaticamente.
     */
    suspend fun buscarPerfil(): LoginResponse =
        withContext(Dispatchers.IO) {

            val resposta = RetrofitClient.instancia
                .create(LoginApiService::class.java)
                .buscarPerfil()

            if (resposta.isSuccessful) {

                return@withContext resposta.body()
                    ?: throw Exception(
                        "O servidor retornou o perfil vazio."
                    )
            }

            val mensagem = extrairMensagemErro(
                resposta.errorBody()?.string()
            ) ?: when (resposta.code()) {

                401 -> "Sua sessão expirou. Faça login novamente."

                403 -> "Você não possui autorização."

                404 -> "Perfil não encontrado."

                in 500..599 ->
                    "O servidor apresentou um erro."

                else -> "Não foi possível carregar o perfil."
            }

            throw Exception(
                "$mensagem (HTTP ${resposta.code()})"
            )
        }

    /**
     * Atualiza nome e e-mail do usuário.
     */
    suspend fun atualizarPerfil(
        nome: String,
        email: String
    ): LoginResponse = withContext(Dispatchers.IO) {

        val request = AtualizarPerfilRequest(
            nome = nome.trim(),
            email = email.trim()
        )

        val resposta = RetrofitClient.instancia
            .create(LoginApiService::class.java)
            .atualizarPerfil(request)

        if (resposta.isSuccessful) {

            val dados = resposta.body()
                ?: throw Exception(
                    "O servidor não retornou o perfil atualizado."
                )

            if (!dados.sucesso) {
                throw Exception(
                    dados.mensagem.ifBlank {
                        "Não foi possível atualizar o perfil."
                    }
                )
            }

            return@withContext dados
        }

        val mensagem = extrairMensagemErro(
            resposta.errorBody()?.string()
        ) ?: when (resposta.code()) {

            400 -> "Dados do perfil inválidos."

            401 -> "Sua sessão expirou. Faça login novamente."

            403 -> "Você não possui autorização."

            404 -> "Usuário não encontrado."

            409 -> "Este e-mail já está sendo utilizado."

            in 500..599 ->
                "O servidor apresentou um erro."

            else -> "Não foi possível atualizar o perfil."
        }

        throw Exception(
            "$mensagem (HTTP ${resposta.code()})"
        )
    }

    /**
     * Altera a senha do usuário autenticado.
     *
     * A API invalida a sessão anterior e
     * retorna um novo token JWT.
     */
    suspend fun alterarSenha(
        senhaAtual: String,
        novaSenha: String,
        confirmarNovaSenha: String
    ): LoginResponse = withContext(Dispatchers.IO) {

        require(senhaAtual.isNotBlank()) {
            "Informe a senha atual."
        }

        require(novaSenha.isNotBlank()) {
            "Informe a nova senha."
        }

        require(novaSenha == confirmarNovaSenha) {
            "As novas senhas não coincidem."
        }

        val request = AlterarSenhaRequest(
            senhaAtual = senhaAtual,
            novaSenha = novaSenha,
            confirmarNovaSenha = confirmarNovaSenha
        )

        val resposta = RetrofitClient.instancia
            .create(LoginApiService::class.java)
            .alterarSenha(request)

        if (resposta.isSuccessful) {

            val dados = resposta.body()
                ?: throw Exception(
                    "O servidor não retornou a confirmação. " +
                            "Verifique sua sessão antes de tentar novamente."
                )

            if (!dados.sucesso) {
                throw Exception(
                    dados.mensagem.ifBlank {
                        "Não foi possível confirmar a alteração."
                    }
                )
            }

            // Devolve o novo JWT para a Activity.
            // A Activity salvará o token no TokenManager.
            return@withContext dados
        }

        val mensagem = extrairMensagemErro(
            resposta.errorBody()?.string()
        ) ?: when (resposta.code()) {

            400 -> "Senha atual incorreta ou sessão inválida."

            401 -> "Sua sessão expirou. Faça login novamente."

            403 -> "Você não possui autorização."

            404 -> "Rota de alteração de senha não encontrada."

            429 -> "Muitas tentativas. Tente novamente mais tarde."

            in 500..599 ->
                "O servidor apresentou um erro interno."

            else -> "Não foi possível alterar sua senha."
        }

        throw Exception(
            "$mensagem (HTTP ${resposta.code()})"
        )
    }

    /**
     * Extrai a mensagem de erro enviada pela API.
     */
    private fun extrairMensagemErro(
        corpo: String?
    ): String? {

        if (corpo.isNullOrBlank()) {
            return null
        }

        return try {

            val resposta = gson.fromJson(
                corpo,
                MensagemResponse::class.java
            )

            resposta?.mensagem
                ?.takeIf { it.isNotBlank() }

        } catch (_: Exception) {
            null
        }
    }
}