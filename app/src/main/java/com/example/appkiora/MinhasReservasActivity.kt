package com.example.appkiora

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MinhasReservasActivity : AppCompatActivity() {

    companion object {

        private const val PREFERENCIAS =
            "kiora_notificacoes_reservas"

        private const val NOTIFICACOES_ATIVAS =
            "notificacoes_ativas"

        private const val CANAL_NOTIFICACOES =
            "kiora_atualizacoes_reservas"

        private const val PREFERENCIAS_FCM =
            "kiora_fcm"

        private const val TOKEN_ATUAL =
            "token_atual"

        private const val TOKEN_REGISTRADO =
            "token_registrado"

        private val STATUS_ATIVOS = setOf(
            0, // Confirmada
            1, // Aguardando
            2, // Chamada
            3, // Em atendimento
            7  // Aguardando chegada
        )
    }

    private val reservasRepository =
        ReservaRepository()

    private val dispositivosPushRepository by lazy {
        DispositivosPushRepository(applicationContext)
    }

    private lateinit var recyclerMinhasReservas:
            RecyclerView

    private lateinit var txtEstadoMinhasReservas:
            TextView

    private lateinit var adapterReservas:
            MinhasReservasAdapter

    private lateinit var btnSair:
            ImageButton

    private lateinit var btnEmAndamento:
            AppCompatButton

    private lateinit var btnHistorico:
            AppCompatButton

    private lateinit var switchNotificacoes:
            SwitchCompat

    private var reservasTodas =
        listOf<ReservaApiResponse>()

    private var mostrandoHistorico = false
    private var carregando = false
    private var cancelando = false
    private var ajustandoSwitch = false

    private var operacaoPushEmAndamento = false

    private val preferencias by lazy {
        getSharedPreferences(
            PREFERENCIAS,
            Context.MODE_PRIVATE
        )
    }

    private val preferenciasFirebase by lazy {
        getSharedPreferences(
            PREFERENCIAS_FCM,
            Context.MODE_PRIVATE
        )
    }

    /**
     * Solicita a permissão nativa de notificações.
     *
     * Antes dessa solicitação, apresentamos
     * o modal personalizado do Kiora.
     */
    private val solicitarNotificacoes =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { permitida ->

            if (permitida) {

                ativarNotificacoes()

            } else {

                preferencias.edit()
                    .putBoolean(
                        NOTIFICACOES_ATIVAS,
                        false
                    )
                    .apply()

                atualizarSwitch(false)

                MensagemHelper.aviso(
                    this,
                    "As notificações não foram autorizadas."
                )
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_minhas_reservas
        )

        RetrofitClient.inicializar(
            applicationContext
        )

        mostrandoHistorico =
            savedInstanceState?.getBoolean(
                "mostrando_historico"
            ) ?: false

        findViewById<AppCompatButton>(
            R.id.btnVoltarMinhasReservas
        ).setOnClickListener {
            finish()
        }

        btnSair = findViewById(
            R.id.btnSair
        )

        btnSair.setOnClickListener {
            abrirModalLogout()
        }

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            insets
        }

        recyclerMinhasReservas =
            findViewById(
                R.id.recyclerMinhasReservas
            )

        txtEstadoMinhasReservas =
            findViewById(
                R.id.txtEstadoMinhasReservas
            )

        configurarListaReservas()
        configurarControles()
        criarCanalNotificacoes()
    }

    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        outState.putBoolean(
            "mostrando_historico",
            mostrandoHistorico
        )

        super.onSaveInstanceState(outState)
    }

    /**
     * Atualiza a tela e sincroniza o token FCM
     * quando as notificações estão autorizadas.
     */
    override fun onResume() {
        super.onResume()

        btnSair.visibility =
            if (estaLogado()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val habilitadas =
            preferencias.getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            ) && notificacoesAutorizadas()

        atualizarSwitch(habilitadas)

        if (habilitadas) {
            prepararTokenFirebase()
        }

        carregarReservas()
    }

    private fun estaLogado(): Boolean {
        return !TokenManager(
            applicationContext
        ).obterToken().isNullOrBlank()
    }

    private fun dp(
        valor: Int
    ): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    /**
     * Configura a lista de reservas existente.
     */
    private fun configurarListaReservas() {

        adapterReservas = MinhasReservasAdapter(
            emptyList(),
            onCancelarClick = { reserva ->
                confirmarCancelamento(reserva)
            }
        )

        recyclerMinhasReservas.layoutManager =
            LinearLayoutManager(this)

        recyclerMinhasReservas.adapter =
            adapterReservas
    }

    /**
     * Adiciona abas, notificações e atualização
     * sem modificar o XML original.
     */
    private fun configurarControles() {

        val corDourada =
            Color.parseColor("#D9B27A")

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                0,
                dp(16),
                0,
                0
            )
        }

        val linhaAbas = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        btnEmAndamento = AppCompatButton(this).apply {
            text = "EM ANDAMENTO"
            textSize = 11f
            isAllCaps = false

            setTextColor(Color.WHITE)
        }

        btnHistorico = AppCompatButton(this).apply {
            text = "HISTÓRICO"
            textSize = 11f
            isAllCaps = false

            setTextColor(Color.WHITE)
        }

        linhaAbas.addView(
            btnEmAndamento,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginEnd = dp(4)
            }
        )

        linhaAbas.addView(
            btnHistorico,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            ).apply {
                marginStart = dp(4)
            }
        )

        container.addView(linhaAbas)

        switchNotificacoes = SwitchCompat(this).apply {

            text =
                "Receber notificações de reservas"

            textSize = 13f
            setTextColor(Color.WHITE)

            setPadding(
                0,
                dp(10),
                0,
                dp(6)
            )
        }

        container.addView(
            switchNotificacoes,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val informacao = TextView(this).apply {

            text =
                "Receba avisos de suas reservas. " +
                        "O envio automático depende da " +
                        "integração do servidor com o Firebase."

            textSize = 11f
            setTextColor(Color.LTGRAY)
        }

        container.addView(informacao)

        val btnAtualizar = TextView(this).apply {

            text = "ATUALIZAR RESERVAS"
            textSize = 12f
            gravity = Gravity.END

            setTextColor(corDourada)

            setPadding(
                0,
                dp(14),
                0,
                dp(4)
            )

            isClickable = true
            isFocusable = true
        }

        btnAtualizar.setOnClickListener {
            carregarReservas()
        }

        container.addView(btnAtualizar)

        val pai =
            txtEstadoMinhasReservas.parent
                    as LinearLayout

        pai.addView(
            container,
            pai.indexOfChild(
                txtEstadoMinhasReservas
            )
        )

        btnEmAndamento.setOnClickListener {

            mostrandoHistorico = false

            atualizarAparenciaAbas()
            exibirReservas()
        }

        btnHistorico.setOnClickListener {

            mostrandoHistorico = true

            atualizarAparenciaAbas()
            exibirReservas()
        }

        val habilitadas =
            preferencias.getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            ) && notificacoesAutorizadas()

        atualizarSwitch(habilitadas)

        /**
         * Controle da ativação e desativação.
         *
         * A preferência não é considerada
         * concluída antes da resposta da API.
         */
        switchNotificacoes.setOnCheckedChangeListener {
                _, marcada ->

            if (ajustandoSwitch) {
                return@setOnCheckedChangeListener
            }

            if (operacaoPushEmAndamento) {

                atualizarSwitch(
                    preferencias.getBoolean(
                        NOTIFICACOES_ATIVAS,
                        false
                    ) && notificacoesAutorizadas()
                )

                MensagemHelper.aviso(
                    this,
                    "Aguarde a conclusão da operação."
                )

                return@setOnCheckedChangeListener
            }

            if (marcada) {

                atualizarSwitch(false)

                confirmarAtivacaoNotificacoes()

            } else {

                atualizarSwitch(true)

                confirmarDesativacaoNotificacoes()
            }
        }

        atualizarAparenciaAbas()
    }

    /**
     * Atualiza a aparência das abas.
     */
    private fun atualizarAparenciaAbas() {

        estilizarAba(
            btnEmAndamento,
            !mostrandoHistorico
        )

        estilizarAba(
            btnHistorico,
            mostrandoHistorico
        )
    }

    private fun estilizarAba(
        botao: AppCompatButton,
        selecionada: Boolean
    ) {

        val fundo = GradientDrawable().apply {

            setColor(
                Color.parseColor(
                    if (selecionada) {
                        "#7D1F1F"
                    } else {
                        "#262626"
                    }
                )
            )

            cornerRadius = dp(9).toFloat()

            setStroke(
                dp(if (selecionada) 2 else 1),
                Color.parseColor("#D9B27A")
            )
        }

        botao.backgroundTintList = null
        botao.background = fundo
    }

    /**
     * Consulta as reservas reais do servidor.
     */
    private fun carregarReservas() {

        if (carregando || cancelando) {
            return
        }

        carregando = true

        txtEstadoMinhasReservas.text =
            "Consultando suas reservas..."

        txtEstadoMinhasReservas.visibility =
            View.VISIBLE

        recyclerMinhasReservas.visibility =
            View.GONE

        lifecycleScope.launch {

            try {

                val reservas =
                    reservasRepository.listarMinhasReservas()

                sincronizarSituacoes(reservas)

                reservasTodas = reservas

                exibirReservas()

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                reservasTodas = emptyList()

                txtEstadoMinhasReservas.text =
                    erro.message
                        ?: "Não foi possível consultar suas reservas."

                txtEstadoMinhasReservas.visibility =
                    View.VISIBLE

                recyclerMinhasReservas.visibility =
                    View.GONE

            } finally {
                carregando = false
            }
        }
    }

    /**
     * Separa reservas ativas das encerradas.
     */
    private fun exibirReservas() {

        val reservasVisiveis =
            reservasTodas.filter { reserva ->

                val ativa =
                    reserva.situacao in STATUS_ATIVOS

                if (mostrandoHistorico) {
                    !ativa
                } else {
                    ativa
                }
            }

        adapterReservas.atualizarReservas(
            reservasVisiveis
        )

        if (reservasVisiveis.isEmpty()) {

            txtEstadoMinhasReservas.text =
                if (mostrandoHistorico) {

                    "Nenhuma reserva no histórico."

                } else {

                    "Nenhuma reserva em andamento."
                }

            txtEstadoMinhasReservas.visibility =
                View.VISIBLE

            recyclerMinhasReservas.visibility =
                View.GONE

        } else {

            txtEstadoMinhasReservas.visibility =
                View.GONE

            recyclerMinhasReservas.visibility =
                View.VISIBLE
        }
    }

    /**
     * Compara situações recebidas pela API
     * com as situações salvas anteriormente.
     */
    private fun sincronizarSituacoes(
        reservas: List<ReservaApiResponse>
    ) {

        val notificacoesAtivas =
            preferencias.getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            ) && notificacoesAutorizadas()

        val editor = preferencias.edit()

        reservas.forEach { reserva ->

            val chave =
                "situacao_${reserva.codigo}"

            val anterior =
                if (preferencias.contains(chave)) {

                    preferencias.getInt(
                        chave,
                        -1
                    )

                } else {
                    null
                }

            if (
                notificacoesAtivas &&
                anterior != null &&
                anterior != reserva.situacao
            ) {
                enviarNotificacao(reserva)
            }

            editor.putInt(
                chave,
                reserva.situacao
            )
        }

        editor.apply()
    }

    /**
     * Altera o switch sem chamar novamente
     * seu listener.
     */
    private fun atualizarSwitch(
        ativado: Boolean
    ) {

        ajustandoSwitch = true

        switchNotificacoes.isChecked = ativado

        ajustandoSwitch = false
    }

    /**
     * Apresenta o modal Kiora antes de solicitar
     * a permissão ou registrar o dispositivo.
     */
    private fun confirmarAtivacaoNotificacoes() {

        MensagemHelper.modalAcao(
            contexto = this,

            titulo = "ATIVAR NOTIFICAÇÕES",

            mensagem = buildString {

                append(
                    "Deseja receber notificações " +
                            "sobre suas reservas no Kiora?"
                )

                append(
                    "\n\nVocê poderá acompanhar " +
                            "mudanças na situação das suas reservas."
                )

                append(
                    "\n\nVocê poderá desativar " +
                            "os avisos quando desejar."
                )
            },

            textoPrincipal = "ATIVAR",

            aoPrincipal = {

                if (
                    Build.VERSION.SDK_INT >= 33 &&
                    !temPermissaoNotificacoes()
                ) {

                    solicitarNotificacoes.launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )

                } else {

                    ativarNotificacoes()
                }
            },

            textoSecundario = "AGORA NÃO",

            aoSecundario = {
                atualizarSwitch(false)
            }
        )
    }

    /**
     * Obtém o token atual do Firebase.
     *
     * Não registra o token em Logcat.
     */
    private suspend fun obterTokenFirebase(): String =
        suspendCancellableCoroutine { continuacao ->

            FirebaseMessaging.getInstance()
                .token
                .addOnCompleteListener { tarefa ->

                    if (!continuacao.isActive) {
                        return@addOnCompleteListener
                    }

                    val token =
                        if (tarefa.isSuccessful) {
                            tarefa.result
                        } else {
                            null
                        }

                    if (!token.isNullOrBlank()) {

                        continuacao.resume(token)

                    } else {

                        continuacao.resumeWithException(
                            tarefa.exception
                                ?: IllegalStateException(
                                    "Não foi possível obter " +
                                            "o token Firebase."
                                )
                        )
                    }
                }
        }

    /**
     * Recupera o token, registra o dispositivo
     * na API e salva qual token foi registrado.
     *
     * Também é utilizado em onResume().
     */
    private fun prepararTokenFirebase(
        mostrarConfirmacao: Boolean = false
    ) {

        if (operacaoPushEmAndamento) {
            return
        }

        val habilitadas =
            preferencias.getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            ) && notificacoesAutorizadas()

        if (!habilitadas) {
            return
        }

        operacaoPushEmAndamento = true

        lifecycleScope.launch {

            try {

                val token = obterTokenFirebase()

                preferenciasFirebase.edit()
                    .putString(
                        TOKEN_ATUAL,
                        token
                    )
                    .apply()

                dispositivosPushRepository
                    .registrarDispositivo(token)

                // Somente marca como registrado
                // após a confirmação da API.
                preferenciasFirebase.edit()
                    .putString(
                        TOKEN_REGISTRADO,
                        token
                    )
                    .apply()

                if (mostrarConfirmacao) {

                    MensagemHelper.sucesso(
                        this@MinhasReservasActivity,
                        "Notificações ativadas e " +
                                "dispositivo registrado."
                    )
                }

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                if (mostrarConfirmacao) {

                    preferencias.edit()
                        .putBoolean(
                            NOTIFICACOES_ATIVAS,
                            false
                        )
                        .apply()

                    atualizarSwitch(false)

                    MensagemHelper.erro(
                        this@MinhasReservasActivity,
                        "Não foi possível registrar " +
                                "o dispositivo.\n" +
                                (erro.message
                                    ?: "Verifique a conexão.")
                    )
                }

            } finally {

                operacaoPushEmAndamento = false
            }
        }
    }

    /**
     * Ativa a preferência local e registra
     * o dispositivo no servidor.
     */
    private fun ativarNotificacoes() {

        if (operacaoPushEmAndamento) {
            return
        }

        if (!notificacoesAutorizadas()) {

            atualizarSwitch(false)

            MensagemHelper.aviso(
                this,
                "Autorize as notificações " +
                        "nas configurações do Android."
            )

            return
        }

        criarCanalNotificacoes()

        val editor = preferencias.edit()

        editor.putBoolean(
            NOTIFICACOES_ATIVAS,
            true
        )

        reservasTodas.forEach { reserva ->

            editor.putInt(
                "situacao_${reserva.codigo}",
                reserva.situacao
            )
        }

        editor.apply()

        atualizarSwitch(true)

        prepararTokenFirebase(
            mostrarConfirmacao = true
        )
    }

    /**
     * Confirma a desativação no modal Kiora.
     */
    private fun confirmarDesativacaoNotificacoes() {

        MensagemHelper.modalAcao(
            contexto = this,

            titulo = "DESATIVAR NOTIFICAÇÕES",

            mensagem =
                "Deseja deixar de receber avisos " +
                        "sobre suas reservas?\n\n" +
                        "Você poderá continuar acompanhando " +
                        "suas reservas normalmente.",

            textoPrincipal = "DESATIVAR",

            aoPrincipal = {
                desativarNotificacoes()
            },

            textoSecundario = "MANTER ATIVADAS",

            aoSecundario = {
                atualizarSwitch(true)
            }
        )
    }

    /**
     * Utiliza preferencialmente o token que
     * foi registrado anteriormente na API.
     *
     * Isso evita depender somente de um token
     * que possa ter sido renovado pelo Firebase.
     */
    private suspend fun obterTokenParaDesativacao(): String {

        val registrado =
            preferenciasFirebase.getString(
                TOKEN_REGISTRADO,
                null
            )

        if (!registrado.isNullOrBlank()) {
            return registrado
        }

        val atual =
            preferenciasFirebase.getString(
                TOKEN_ATUAL,
                null
            )

        if (!atual.isNullOrBlank()) {
            return atual
        }

        return obterTokenFirebase()
    }

    /**
     * Desativa o dispositivo no backend.
     *
     * A preferência é desligada somente
     * depois da confirmação do servidor.
     */
    private fun desativarNotificacoes() {

        if (operacaoPushEmAndamento) {
            return
        }

        operacaoPushEmAndamento = true

        lifecycleScope.launch {

            try {

                val token =
                    obterTokenParaDesativacao()

                dispositivosPushRepository
                    .desativarDispositivo(token)

                preferencias.edit()
                    .putBoolean(
                        NOTIFICACOES_ATIVAS,
                        false
                    )
                    .apply()

                preferenciasFirebase.edit()
                    .remove(TOKEN_REGISTRADO)
                    .apply()

                atualizarSwitch(false)

                MensagemHelper.sucesso(
                    this@MinhasReservasActivity,
                    "Notificações desativadas " +
                            "com sucesso."
                )

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                atualizarSwitch(true)

                MensagemHelper.erro(
                    this@MinhasReservasActivity,
                    "O servidor não confirmou " +
                            "a desativação.\n" +
                            (erro.message
                                ?: "Tente novamente.")
                )

            } finally {

                operacaoPushEmAndamento = false
            }
        }
    }

    /**
     * Verifica a permissão Android.
     */
    private fun temPermissaoNotificacoes(): Boolean {

        return Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Verifica a permissão e se o Android
     * permite exibir notificações.
     */
    private fun notificacoesAutorizadas(): Boolean {

        return temPermissaoNotificacoes() &&
                NotificationManagerCompat
                    .from(this)
                    .areNotificationsEnabled()
    }

    /**
     * Canal utilizado pelas notificações
     * locais e pelo serviço Firebase.
     */
    private fun criarCanalNotificacoes() {

        if (Build.VERSION.SDK_INT >= 26) {

            val canal = NotificationChannel(
                CANAL_NOTIFICACOES,
                "Atualizações de reservas Kiora",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {

                description =
                    "Avisos sobre mudanças nas reservas."
            }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(canal)
        }
    }

    /**
     * Exibe uma notificação local quando
     * a consulta detecta mudança de situação.
     */
    private fun enviarNotificacao(
        reserva: ReservaApiResponse
    ) {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val permissao =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                permissao !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val gerenciador =
            NotificationManagerCompat.from(this)

        if (!gerenciador.areNotificationsEnabled()) {
            return
        }

        val intent = Intent(
            this,
            MinhasReservasActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                reserva.codigo.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        val notificacao =
            NotificationCompat.Builder(
                this,
                CANAL_NOTIFICACOES
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(
                    "Kiora — Reserva atualizada"
                )
                .setContentText(
                    "Nova situação: ${reserva.situacaoTexto}"
                )
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .build()

        try {

            gerenciador.notify(
                reserva.codigo.hashCode(),
                notificacao
            )

        } catch (_: SecurityException) {

            preferencias.edit()
                .putBoolean(
                    NOTIFICACOES_ATIVAS,
                    false
                )
                .apply()

            atualizarSwitch(false)
        }
    }

    /**
     * Atualiza a reserva antes de apresentar
     * a confirmação de cancelamento.
     */
    private fun confirmarCancelamento(
        reserva: ReservaApiResponse
    ) {

        if (cancelando || carregando) {
            return
        }

        if (!reserva.podeCancelar) {

            MensagemHelper.aviso(
                this,
                "Esta reserva não permite cancelamento."
            )

            return
        }

        cancelando = true

        lifecycleScope.launch {

            try {

                val atual =
                    reservasRepository.consultarReserva(
                        reserva.codigo
                    )

                if (!atual.podeCancelar) {

                    MensagemHelper.aviso(
                        this@MinhasReservasActivity,
                        "Esta reserva não pode mais ser cancelada."
                    )

                    return@launch
                }

                MensagemHelper.modalAcao(
                    contexto =
                        this@MinhasReservasActivity,

                    titulo = "CANCELAR RESERVA",

                    mensagem = buildString {

                        append(
                            "Deseja realmente cancelar sua reserva?"
                        )

                        append(
                            "\n\nSituação: ${atual.situacaoTexto}"
                        )

                        append(
                            "\nPessoas: ${atual.pessoas}"
                        )

                        append(
                            "\n\nEsta ação não poderá ser desfeita."
                        )
                    },

                    textoPrincipal = "CANCELAR",

                    aoPrincipal = {
                        realizarCancelamento(atual)
                    },

                    textoSecundario = "VOLTAR"
                )

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@MinhasReservasActivity,
                    erro.message
                        ?: "Não foi possível consultar a reserva."
                )

            } finally {

                cancelando = false
            }
        }
    }

    /**
     * Cancela a reserva pela API e abre
     * o histórico após a confirmação.
     */
    private fun realizarCancelamento(
        reserva: ReservaApiResponse
    ) {

        if (cancelando || carregando) {
            return
        }

        cancelando = true

        lifecycleScope.launch {

            try {

                val resultado =
                    reservasRepository.cancelarReserva(
                        codigo = reserva.codigo,
                        versao = reserva.versao
                    )

                if (resultado.sucesso) {

                    preferencias.edit()
                        .putInt(
                            "situacao_${reserva.codigo}",
                            5
                        )
                        .apply()

                    mostrandoHistorico = true

                    atualizarAparenciaAbas()

                    MensagemHelper.sucesso(
                        this@MinhasReservasActivity,
                        "Reserva cancelada com sucesso!"
                    )
                }

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@MinhasReservasActivity,
                    erro.message
                        ?: "Não foi possível cancelar a reserva."
                )

            } finally {

                cancelando = false
                carregarReservas()
            }
        }
    }

    /**
     * Modal de logout padronizado.
     */
    private fun abrirModalLogout() {

        MensagemHelper.modalAcao(
            contexto = this,

            titulo = "SAIR DA CONTA",

            mensagem =
                "Deseja realmente sair da sua conta?",

            textoPrincipal = "SAIR",

            aoPrincipal = {
                sairDaContaComDesvinculacao()
            },

            textoSecundario = "CANCELAR"
        )
    }

    /**
     * Tenta desativar o vínculo do dispositivo
     * antes de remover a autenticação JWT.
     */
    private fun sairDaContaComDesvinculacao() {

        if (operacaoPushEmAndamento) {

            MensagemHelper.aviso(
                this,
                "Aguarde a conclusão da operação atual."
            )

            return
        }

        operacaoPushEmAndamento = true

        lifecycleScope.launch {

            try {

                val tokenRegistrado =
                    preferenciasFirebase.getString(
                        TOKEN_REGISTRADO,
                        null
                    )

                val tokenAtual =
                    preferenciasFirebase.getString(
                        TOKEN_ATUAL,
                        null
                    )

                val token =
                    tokenRegistrado ?: tokenAtual

                if (!token.isNullOrBlank()) {

                    dispositivosPushRepository
                        .desativarDispositivo(token)
                }

                // A API confirmou a desativação,
                // ou não havia token salvo.
                preferenciasFirebase.edit()
                    .remove(TOKEN_REGISTRADO)
                    .apply()

                finalizarLogout()

            } catch (erro: CancellationException) {
                throw erro

            } catch (_: Exception) {

                MensagemHelper.modalAcao(
                    contexto =
                        this@MinhasReservasActivity,

                    titulo = "AVISO DE SEGURANÇA",

                    mensagem =
                        "Não foi possível desvincular " +
                                "o dispositivo do servidor.\n\n" +
                                "Se sair agora, o servidor poderá " +
                                "continuar enviando notificações " +
                                "para esta conta.\n\n" +
                                "Deseja sair mesmo assim?",

                    textoPrincipal =
                        "SAIR MESMO ASSIM",

                    aoPrincipal = {
                        finalizarLogout()
                    },

                    textoSecundario =
                        "TENTAR NOVAMENTE",

                    aoSecundario = {
                        sairDaContaComDesvinculacao()
                    }
                )

            } finally {

                operacaoPushEmAndamento = false
            }
        }
    }

    /**
     * Encerra a sessão local.
     *
     * A chave de visitante não é excluída,
     * preservando as reservas feitas sem login.
     */
    private fun finalizarLogout() {

        preferencias.edit()
            .putBoolean(
                NOTIFICACOES_ATIVAS,
                false
            )
            .apply()

        TokenManager(this).limparToken()

        val intent = Intent(
            this,
            LoginActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        finish()
    }
}