package com.example.appkiora

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Gerencia o registro e a desativação de
 * dispositivos Firebase na API Kiora.
 *
 * Reutiliza:
 * - RetrofitClient
 * - Sessão de reservas
 * - Identificação de visitantes
 * - Autenticação JWT
 * - Proteção CSRF
 */
class DispositivosPushRepository(
    context: Context
) {

    private val contexto = context.applicationContext

    companion object {

        private const val PREFERENCIAS =
            "kiora_notificacoes_reservas"

        private const val NOTIFICACOES_ATIVAS =
            "notificacoes_ativas"
    }

    private val api by lazy {

        RetrofitClient.instancia.create(
            DispositivosPushApiService::class.java
        )
    }

    private val reservaRepository by lazy {
        ReservaRepository()
    }

    init {

        RetrofitClient.inicializar(contexto)
    }

    /**
     * Verifica se o usuário autorizou
     * notificações no Kiora e no Android.
     */
    private fun notificacoesPermitidas(): Boolean {

        val preferenciaAtiva =
            contexto.getSharedPreferences(
                PREFERENCIAS,
                Context.MODE_PRIVATE
            ).getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            )

        if (!preferenciaAtiva) {
            return false
        }

        if (Build.VERSION.SDK_INT >= 33) {

            val permissao =
                ContextCompat.checkSelfPermission(
                    contexto,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permissao !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return NotificationManagerCompat
            .from(contexto)
            .areNotificationsEnabled()
    }

    /**
     * Valida o token FCM antes de enviá-lo.
     *
     * Utiliza os mesmos limites definidos
     * no controller ASP.NET Core.
     */
    private fun validarToken(
        token: String
    ): String {

        val tokenFormatado = token.trim()

        require(
            tokenFormatado.length in 20..1024 &&
                    tokenFormatado.all {
                        it.code in 33..126
                    }
        ) {
            "O token Firebase do dispositivo é inválido."
        }

        return tokenFormatado
    }

    /**
     * Registra o token FCM na API.
     *
     * Somente registra quando as
     * notificações estão autorizadas.
     *
     * A API associa o dispositivo ao
     * visitante ou usuário autenticado.
     */
    suspend fun registrarDispositivo(
        token: String
    ): Boolean = withContext(Dispatchers.IO) {

        val tokenValidado = validarToken(token)

        if (!notificacoesPermitidas()) {

            throw IllegalStateException(
                "As notificações precisam estar " +
                        "ativadas no Kiora e autorizadas " +
                        "nas configurações do Android."
            )
        }

        // Obtém a identificação do visitante
        // e atualiza a proteção CSRF.
        reservaRepository.iniciarSessao()

        val resposta = api.registrarDispositivo(
            DispositivoPushRequest(
                token = tokenValidado
            )
        )

        if (!resposta.isSuccessful) {

            throw IOException(
                extrairErro(
                    resposta.code(),
                    resposta.errorBody()?.string()
                )
            )
        }

        val resultado = resposta.body()
            ?: throw IOException(
                "O servidor não retornou a confirmação " +
                        "do registro do dispositivo."
            )

        if (!resultado.sucesso) {

            throw IllegalStateException(
                "O servidor não confirmou o registro " +
                        "do dispositivo Firebase."
            )
        }

        true
    }

    /**
     * Desativa o token FCM na API.
     *
     * A desativação NÃO exige que a
     * preferência de notificações esteja ativa.
     *
     * Isso permite desvincular o dispositivo
     * mesmo após o usuário desativar os avisos.
     */
    suspend fun desativarDispositivo(
        token: String
    ): Boolean = withContext(Dispatchers.IO) {

        val tokenValidado = validarToken(token)

        // Mantém a identificação necessária
        // para a API autorizar a desativação.
        reservaRepository.iniciarSessao()

        val resposta = api.desativarDispositivo(
            DispositivoPushRequest(
                token = tokenValidado
            )
        )

        if (!resposta.isSuccessful) {

            throw IOException(
                extrairErro(
                    resposta.code(),
                    resposta.errorBody()?.string()
                )
            )
        }

        val resultado = resposta.body()
            ?: throw IOException(
                "O servidor não retornou a confirmação " +
                        "de desativação do dispositivo."
            )

        if (!resultado.sucesso) {

            throw IllegalStateException(
                "O servidor não confirmou a desativação " +
                        "das notificações."
            )
        }

        true
    }

    /**
     * Interpreta os erros retornados pela API.
     *
     * Não inclui o token FCM nas mensagens.
     */
    private fun extrairErro(
        codigoHttp: Int,
        conteudo: String?
    ): String {

        if (!conteudo.isNullOrBlank()) {

            try {

                val json = JsonParser
                    .parseString(conteudo)

                if (json.isJsonObject) {

                    val mensagem = json.asJsonObject
                        .get("mensagem")

                    if (
                        mensagem != null &&
                        mensagem.isJsonPrimitive &&
                        mensagem.asString.isNotBlank()
                    ) {
                        return mensagem.asString
                    }
                }

            } catch (_: Exception) {

                // Utiliza a mensagem padrão.
            }
        }

        return when (codigoHttp) {

            400 ->
                "Solicitação inválida ou sessão CSRF expirada."

            401 ->
                "Não foi possível identificar sua sessão."

            403 ->
                "Você não possui autorização para esta operação."

            404 ->
                "O serviço de notificações não foi encontrado."

            409 ->
                "O dispositivo não pôde ser registrado. " +
                        "Tente novamente."

            429 ->
                "Muitas solicitações. Tente novamente mais tarde."

            in 500..599 ->
                "O servidor Kiora apresentou um erro interno."

            else ->
                "Não foi possível comunicar com o servidor. " +
                        "HTTP $codigoHttp."
        }
    }
}