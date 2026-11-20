package com.example.appkiora

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Gerencia a comunicação de reservas
 * entre o aplicativo Android e a API Kiora.
 *
 * Funcionalidades:
 * - Sessão do visitante
 * - Consulta de horários
 * - Criação de reservas
 * - Listagem de reservas
 * - Acompanhamento de reservas
 * - Cancelamento de reservas
 */
class ReservaRepository {

    private val api by lazy {
        RetrofitClient.instancia
            .create(ReservaApiService::class.java)
    }

    /**
     * Inicia ou recupera a sessão do visitante.
     *
     * Obtém a chave de identificação e o token CSRF.
     */
    suspend fun iniciarSessao(): SessaoReservaApiResponse =
        withContext(Dispatchers.IO) {

            val resposta = api.abrirSessao()

            if (!resposta.isSuccessful) {
                throw Exception(
                    extrairErro(
                        resposta.code(),
                        resposta.errorBody()?.string()
                    )
                )
            }

            val sessao = resposta.body()
                ?: throw Exception(
                    "O servidor retornou uma sessão vazia."
                )

            RetrofitClient.registrarSessaoReserva(sessao)

            sessao
        }

    /**
     * Consulta os horários disponíveis na API.
     */
    suspend fun consultarHorarios(
        data: String,
        quantidadePessoas: Int
    ): List<HorarioReservaApiResponse> =
        withContext(Dispatchers.IO) {

            require(
                Regex("""\d{4}-\d{2}-\d{2}""").matches(data)
            ) {
                "Informe a data no formato AAAA-MM-DD."
            }

            require(quantidadePessoas in 1..6) {
                "Reservas online permitem de 1 a 6 pessoas."
            }

            iniciarSessao()

            val resposta = api.consultarHorarios(
                data = data,
                pessoas = quantidadePessoas
            )

            if (!resposta.isSuccessful) {
                throw Exception(
                    extrairErro(
                        resposta.code(),
                        resposta.errorBody()?.string()
                    )
                )
            }

            resposta.body()
                ?: throw Exception(
                    "O servidor não retornou os horários."
                )
        }

    /**
     * Prepara os dados informados pelo cliente.
     *
     * Mantido para preservar as chamadas existentes.
     */
    fun prepararReserva(
        data: String,
        horario: String,
        quantidadePessoas: Int,
        observacoes: String?
    ): ReservaRequest {

        require(data.isNotBlank()) {
            "A data da reserva é obrigatória."
        }

        require(horario.isNotBlank()) {
            "O horário da reserva é obrigatório."
        }

        require(quantidadePessoas in 1..6) {
            "Reservas online permitem de 1 a 6 pessoas."
        }

        return ReservaRequest(
            data = data,
            horario = horario,
            quantidadePessoas = quantidadePessoas,
            observacoes = observacoes
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Cria uma reserva no restaurante.
     *
     * A mesma chaveSolicitacao deve ser reutilizada
     * caso seja necessário reenviar a solicitação.
     */
    suspend fun criarReserva(
        nome: String,
        telefone: String,
        data: String,
        horario: String,
        quantidadePessoas: Int,
        chaveSolicitacao: String
    ): ReservaApiResponse = withContext(Dispatchers.IO) {

        val nomeFormatado = nome.trim()

        val telefoneFormatado = telefone.filter {
            it in '0'..'9'
        }

        require(nomeFormatado.length in 2..100) {
            "Informe um nome entre 2 e 100 caracteres."
        }

        require(telefoneFormatado.length in 10..11) {
            "Informe um telefone com DDD válido."
        }

        require(quantidadePessoas in 1..6) {
            "Reservas online permitem de 1 a 6 pessoas."
        }

        require(
            Regex("""\d{4}-\d{2}-\d{2}""").matches(data)
        ) {
            "A data deve estar no formato AAAA-MM-DD."
        }

        require(
            Regex("""(?:[01]\d|2[0-3]):[0-5]\d""")
                .matches(horario)
        ) {
            "Informe um horário válido no formato HH:mm."
        }

        val chave = validarUuid(
            chaveSolicitacao,
            "Identificação da solicitação inválida."
        )

        require(chave != UUID(0L, 0L)) {
            "Identificação da solicitação inválida."
        }

        iniciarSessao()

        val dados = CriarReservaApiRequest(
            chaveSolicitacao = chave.toString(),
            nome = nomeFormatado,
            telefone = telefoneFormatado,
            pessoas = quantidadePessoas,
            data = data,
            horario = horario,
            aceitaPermanenciaReduzida = false
        )

        val resposta = try {
            api.criarReserva(dados)
        } catch (erro: IOException) {
            throw IOException(
                "Não foi possível confirmar o resultado da " +
                        "solicitação. Consulte suas reservas " +
                        "antes de tentar novamente. Se reenviar, " +
                        "utilize a mesma identificação.",
                erro
            )
        }

        if (!resposta.isSuccessful) {
            throw Exception(
                extrairErro(
                    resposta.code(),
                    resposta.errorBody()?.string()
                )
            )
        }

        resposta.body()
            ?: throw Exception(
                "O servidor não retornou a confirmação. " +
                        "Consulte a reserva antes de reenviar."
            )
    }

    /**
     * NOVO
     *
     * Lista as reservas associadas ao visitante
     * ou ao cliente autenticado.
     *
     * A API retorna até 50 reservas recentes.
     */
    suspend fun listarMinhasReservas():
            List<ReservaApiResponse> =
        withContext(Dispatchers.IO) {

            // Garante a identificação do visitante.
            iniciarSessao()

            val resposta = api.listarMinhasReservas()

            if (!resposta.isSuccessful) {
                throw Exception(
                    extrairErro(
                        resposta.code(),
                        resposta.errorBody()?.string()
                    )
                )
            }

            resposta.body()
                ?: throw Exception(
                    "O servidor não retornou a lista de reservas."
                )
        }

    /**
     * NOVO
     *
     * Consulta uma reserva específica.
     *
     * O servidor verifica se a reserva pertence
     * ao visitante ou ao cliente autenticado.
     */
    suspend fun consultarReserva(
        codigo: String
    ): ReservaApiResponse =
        withContext(Dispatchers.IO) {

            val codigoValidado = validarUuid(
                codigo,
                "Código da reserva inválido."
            ).toString()

            // Recupera a sessão antes da consulta.
            iniciarSessao()

            val resposta = api.consultarReserva(
                codigo = codigoValidado
            )

            if (!resposta.isSuccessful) {
                throw Exception(
                    extrairErro(
                        resposta.code(),
                        resposta.errorBody()?.string()
                    )
                )
            }

            resposta.body()
                ?: throw Exception(
                    "O servidor não retornou os dados da reserva."
                )
        }

    /**
     * NOVO
     *
     * Cancela uma reserva existente.
     *
     * A API exige:
     * - Código UUID
     * - Versão UUID atual
     * - Identificação do visitante ou usuário
     * - Token CSRF válido
     *
     * A confirmação ocorre somente após o
     * servidor retornar sucesso = true.
     */
    suspend fun cancelarReserva(
        codigo: String,
        versao: String
    ): CancelarReservaApiResponse =
        withContext(Dispatchers.IO) {

            val codigoValidado = validarUuid(
                codigo,
                "Código da reserva inválido."
            ).toString()

            val versaoValidada = validarUuid(
                versao,
                "Versão da reserva inválida."
            ).toString()

            // Garante a sessão e atualiza o CSRF.
            iniciarSessao()

            val request = CancelarReservaApiRequest(
                codigo = codigoValidado,
                versao = versaoValidada
            )

            val resposta = try {
                api.cancelarReserva(request)
            } catch (erro: IOException) {
                throw IOException(
                    "Não foi possível confirmar o cancelamento. " +
                            "Consulte a situação atual da reserva " +
                            "antes de tentar novamente.",
                    erro
                )
            }

            if (!resposta.isSuccessful) {
                throw Exception(
                    extrairErro(
                        resposta.code(),
                        resposta.errorBody()?.string()
                    )
                )
            }

            val resultado = resposta.body()
                ?: throw Exception(
                    "O servidor não retornou a confirmação. " +
                            "Consulte a situação da reserva."
                )

            if (!resultado.sucesso) {
                throw Exception(
                    "O servidor não confirmou o cancelamento."
                )
            }

            resultado
        }

    /**
     * Valida códigos UUID utilizados pela API.
     */
    private fun validarUuid(
        valor: String,
        mensagemErro: String
    ): UUID {

        return try {

            val texto = valor.trim()
            val uuid = UUID.fromString(texto)

            require(
                uuid.toString().equals(
                    texto,
                    ignoreCase = true
                )
            )

            uuid

        } catch (_: IllegalArgumentException) {

            throw IllegalArgumentException(mensagemErro)
        }
    }

    /**
     * Interpreta mensagens de erro retornadas pela API.
     */
    private fun extrairErro(
        codigo: Int,
        conteudo: String?
    ): String {

        if (!conteudo.isNullOrBlank()) {

            try {

                val json = JsonParser
                    .parseString(conteudo)
                    .asJsonObject

                // Erros das regras de negócio.
                val mensagem = json.get("mensagem")

                if (
                    mensagem != null &&
                    !mensagem.isJsonNull &&
                    mensagem.asString.isNotBlank()
                ) {
                    return mensagem.asString
                }

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

                    if (mensagens.isNotEmpty()) {
                        return mensagens.joinToString("\n")
                    }
                }

            } catch (_: Exception) {
                // Mantém a mensagem padrão.
            }
        }

        return when (codigo) {

            400 -> "Os dados enviados são inválidos."

            401 -> "Não foi possível identificar sua sessão."

            403 -> "Você não possui autorização para esta operação."

            404 -> "Reserva não encontrada ou acesso não autorizado."

            409 -> "A reserva foi modificada ou a operação " +
                    "não é permitida. Atualize seus dados."

            429 -> "Muitas solicitações. Tente novamente mais tarde."

            in 500..599 ->
                "O servidor Kiora apresentou um erro interno."

            else ->
                "Falha na comunicação com o servidor. " +
                        "Código HTTP: $codigo"
        }
    }
}