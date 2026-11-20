package com.example.appkiora

import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Responsável pelo cadastro de clientes
 * através da API do restaurante Kiora.
 */
class CadastroRepository {

    private val api by lazy {
        RetrofitClient.instancia
            .create(CadastroApiService::class.java)
    }

    /**
     * Prepara e valida os dados do cadastro.
     */
    fun prepararCadastro(
        nome: String,
        email: String,
        senha: String,
        confirmarSenha: String,
        aceiteTermos: Boolean
    ): CadastroRequest {

        val nomeFormatado = nome.trim()
        val emailFormatado = email.trim()

        require(nomeFormatado.isNotBlank()) {
            "O nome é obrigatório."
        }

        require(nomeFormatado.length <= 100) {
            "O nome deve ter no máximo 100 caracteres."
        }

        require(emailFormatado.isNotBlank()) {
            "O e-mail é obrigatório."
        }

        require(emailFormatado.length <= 150) {
            "O e-mail deve ter no máximo 150 caracteres."
        }

        require(senha.length >= 8) {
            "A senha deve ter pelo menos 8 caracteres."
        }

        require(
            senha.any { it.isLetter() } &&
                    senha.any { it in '0'..'9' }
        ) {
            "A senha deve conter letras e números."
        }

        require(senha == confirmarSenha) {
            "As senhas não coincidem."
        }

        require(aceiteTermos) {
            "É necessário aceitar os Termos de Uso " +
                    "e a Política de Privacidade."
        }

        return CadastroRequest(
            nome = nomeFormatado,
            email = emailFormatado,
            senha = senha,
            confirmarSenha = confirmarSenha,
            aceiteTermos = aceiteTermos
        )
    }

    /**
     * Envia o cadastro ao servidor.
     *
     * HTTP 201: cadastro realizado.
     * HTTP 400: dados inválidos.
     * HTTP 409: e-mail existente.
     */
    suspend fun cadastrar(
        request: CadastroRequest
    ): ResultadoCadastro = withContext(Dispatchers.IO) {

        try {

            val resposta = api.cadastrar(request)

            if (resposta.isSuccessful) {

                val resultado = resposta.body()

                if (resultado?.sucesso == true) {

                    ResultadoCadastro.Sucesso(
                        resultado.mensagem.ifBlank {
                            "Cadastro realizado com sucesso!"
                        }
                    )

                } else {

                    ResultadoCadastro.ErroGeral(
                        "O servidor não retornou uma " +
                                "confirmação válida do cadastro."
                    )
                }

            } else {

                val mensagemApi = extrairMensagemErro(
                    resposta.errorBody()?.string()
                )

                when (resposta.code()) {

                    400 -> ResultadoCadastro.DadosInvalidos(
                        mensagemApi
                            ?: "Verifique os dados informados."
                    )

                    409 -> ResultadoCadastro.EmailExistente(
                        mensagemApi
                            ?: "Este e-mail já está cadastrado."
                    )

                    429 -> ResultadoCadastro.ErroGeral(
                        "Muitas solicitações. " +
                                "Tente novamente mais tarde."
                    )

                    in 500..599 -> ResultadoCadastro.ErroGeral(
                        "O servidor Kiora apresentou um erro. " +
                                "Verifique se a conta foi criada " +
                                "antes de tentar novamente. " +
                                "(HTTP ${resposta.code()})"
                    )

                    else -> ResultadoCadastro.ErroGeral(
                        mensagemApi
                            ?: "Não foi possível concluir o cadastro. " +
                            "HTTP ${resposta.code()}."
                    )
                }
            }

        } catch (erro: CancellationException) {
            throw erro

        } catch (_: IOException) {

            ResultadoCadastro.ErroConexao(
                "Não foi possível confirmar o cadastro. " +
                        "Verifique sua conexão e se a conta foi " +
                        "criada antes de tentar novamente."
            )

        } catch (_: Exception) {

            ResultadoCadastro.ErroGeral(
                "Ocorreu um erro inesperado ao processar " +
                        "o cadastro."
            )
        }
    }

    /**
     * Interpreta as mensagens retornadas pelo ASP.NET Core.
     */
    private fun extrairMensagemErro(
        corpo: String?
    ): String? {

        if (corpo.isNullOrBlank()) {
            return null
        }

        return try {

            val json = JsonParser
                .parseString(corpo)
                .asJsonObject

            // Mensagem padrão da API Kiora.
            val mensagem = json.get("mensagem")

            if (mensagem != null &&
                !mensagem.isJsonNull &&
                mensagem.asString.isNotBlank()
            ) {
                mensagem.asString

            } else {

                // Erros de validação do ASP.NET Core.
                val erros = json.get("errors")

                if (erros != null && erros.isJsonObject) {

                    val mensagens = erros.asJsonObject
                        .entrySet()
                        .flatMap { entrada ->
                            entrada.value.asJsonArray.map {
                                it.asString
                            }
                        }

                    mensagens.takeIf { it.isNotEmpty() }
                        ?.joinToString("\n")

                } else {
                    null
                }
            }

        } catch (_: Exception) {
            null
        }
    }
}