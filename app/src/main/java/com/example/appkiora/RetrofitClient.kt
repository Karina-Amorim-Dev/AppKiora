package com.example.appkiora

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Configuração central de comunicação com a API Kiora.
 *
 * Suporta:
 * - Autenticação JWT
 * - Reservas de visitantes sem login
 * - Cookies de sessão
 * - Proteção CSRF
 */
object RetrofitClient {

    private lateinit var contexto: Context

    private const val PREFERENCIAS_RESERVA = "kiora_reservas"
    private const val CHAVE_VISITANTE = "chave_visitante"

    @Volatile
    private var csrfToken: String? = null

    // Cookies utilizados durante a sessão HTTP.
    private val cookiesMemoria = mutableListOf<Cookie>()

    private val cookieJar = object : CookieJar {

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>
        ) {
            synchronized(cookiesMemoria) {
                for (cookie in cookies) {

                    cookiesMemoria.removeAll {
                        it.name == cookie.name &&
                                it.domain == cookie.domain &&
                                it.path == cookie.path
                    }

                    cookiesMemoria.add(cookie)
                }
            }
        }

        override fun loadForRequest(
            url: HttpUrl
        ): List<Cookie> {

            return synchronized(cookiesMemoria) {

                val agora = System.currentTimeMillis()

                cookiesMemoria.removeAll {
                    it.expiresAt <= agora
                }

                cookiesMemoria.filter {
                    it.matches(url)
                }.toList()
            }
        }
    }

    /**
     * Inicializa o Retrofit com o contexto da aplicação.
     */
    fun inicializar(context: Context) {
        contexto = context.applicationContext
    }

    /**
     * Registra a sessão retornada pela API.
     *
     * Deve ser chamado após GET /api/reservas/sessao.
     */
    fun registrarSessaoReserva(
        sessao: SessaoReservaApiResponse
    ) {

        check(::contexto.isInitialized) {
            "O Retrofit precisa ser inicializado."
        }

        require(
            sessao.chaveVisitante.matches(
                Regex("^[0-9a-fA-F]{64}$")
            )
        ) {
            "Chave de visitante inválida."
        }

        require(sessao.csrfToken.isNotBlank()) {
            "Token CSRF inválido."
        }

        // Guarda o token CSRF apenas em memória.
        csrfToken = sessao.csrfToken

        // Mantém a chave do visitante para futuras sessões.
        contexto.getSharedPreferences(
            PREFERENCIAS_RESERVA,
            Context.MODE_PRIVATE
        ).edit()
            .putString(
                CHAVE_VISITANTE,
                sessao.chaveVisitante
            )
            .apply()
    }

    /**
     * Recupera a identificação do visitante.
     */
    private fun obterChaveVisitante(): String? {

        if (!::contexto.isInitialized) {
            return null
        }

        val chave = contexto.getSharedPreferences(
            PREFERENCIAS_RESERVA,
            Context.MODE_PRIVATE
        ).getString(CHAVE_VISITANTE, null)

        return chave?.takeIf {
            it.matches(Regex("^[0-9a-fA-F]{64}$"))
        }
    }

    /**
     * Cria o cliente HTTP usado pelo aplicativo.
     */
    private fun criar(baseUrl: String): Retrofit {

        val clienteHttp = OkHttpClient.Builder()

            // Tempo máximo para estabelecer conexão.
            .connectTimeout(30, TimeUnit.SECONDS)

            // Tempo de espera pela resposta do servidor.
            .readTimeout(90, TimeUnit.SECONDS)

            // Tempo máximo de escrita da requisição.
            .writeTimeout(30, TimeUnit.SECONDS)

            // Tempo máximo total da chamada HTTP.
            .callTimeout(120, TimeUnit.SECONDS)

            // Mantém cookies entre as requisições.
            .cookieJar(cookieJar)

            // Adiciona os cabeçalhos necessários.
            .addInterceptor { cadeia ->

                val original = cadeia.request()

                val requisicao = original.newBuilder()

                // Autenticação dos usuários logados.
                val token = if (::contexto.isInitialized) {
                    TokenManager(contexto).obterToken()
                } else {
                    null
                }

                if (!token.isNullOrBlank()) {
                    requisicao.header(
                        "Authorization",
                        "Bearer $token"
                    )
                }

                // Verifica se a requisição é de reservas.
                val caminho = original.url.encodedPath

                val ehReserva =
                    caminho == "/api/reservas" ||
                            caminho.startsWith("/api/reservas/")

                if (ehReserva) {

                    // Identificação do visitante.
                    obterChaveVisitante()?.let { chave ->
                        requisicao.header(
                            "X-Reserva-Chave",
                            chave
                        )
                    }

                    // Identifica operações de escrita.
                    val operacaoEscrita = original.method in
                            setOf("POST", "PUT", "PATCH", "DELETE")

                    if (operacaoEscrita) {

                        csrfToken?.let { csrf ->
                            requisicao.header(
                                "X-CSRF-TOKEN",
                                csrf
                            )
                        }
                    }
                }

                cadeia.proceed(requisicao.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clienteHttp)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    /**
     * Instância compartilhada por todos os serviços.
     */
    val instancia: Retrofit by lazy {
        criar(ApiConfig.BASE_URL)
    }
}