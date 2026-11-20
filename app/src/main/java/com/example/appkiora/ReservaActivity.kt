package com.example.appkiora

import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity

class ReservaActivity : AppCompatActivity() {

    private val repository = ReservaRepository()

    private lateinit var edtNome: EditText
    private lateinit var edtTelefone: EditText
    private lateinit var edtQuantidade: EditText
    private lateinit var txtDataHorario: TextView
    private lateinit var btnConfirmar: AppCompatButton

    private var dataSelecionada: String? = null
    private var horarioSelecionado: String? = null
    private var dataExibicao: String? = null

    private var carregando = false
    private var reservaConfirmada = false

    private val fusoRestaurante =
        TimeZone.getTimeZone("America/Sao_Paulo")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_reserva)

        RetrofitClient.inicializar(applicationContext)

        // Ajuste das barras do sistema.
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

        // Campos do formulário existente.
        edtNome = findViewById(R.id.edtNomeReserva)

        edtTelefone = findViewById(
            R.id.edtTelefoneReserva
        )

        edtQuantidade = findViewById(
            R.id.edtQuantidadePessoas
        )

        txtDataHorario = findViewById(
            R.id.txtDataHorarioReserva
        )

        btnConfirmar = findViewById(
            R.id.btnConfirmarReserva
        )

        // Quando a quantidade muda, o horário
        // selecionado anteriormente é invalidado.
        edtQuantidade.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    limparDataHorario()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        // Abre o calendário e consulta os horários.
        txtDataHorario.setOnClickListener {

            if (!carregando && !reservaConfirmada) {
                iniciarSelecaoData()
            }
        }

        // Valida o formulário antes de enviar.
        btnConfirmar.setOnClickListener {

            if (!carregando && !reservaConfirmada) {
                validarEConfirmar()
            }
        }
    }

    /**
     * Preserva a seleção quando o Android
     * recria a Activity.
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putString(
            "dataReserva",
            dataSelecionada
        )

        outState.putString(
            "horarioReserva",
            horarioSelecionado
        )

        outState.putString(
            "dataExibicao",
            dataExibicao
        )

        outState.putBoolean(
            "reservaConfirmada",
            reservaConfirmada
        )

        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(
        savedInstanceState: Bundle
    ) {

        super.onRestoreInstanceState(
            savedInstanceState
        )

        dataSelecionada =
            savedInstanceState.getString("dataReserva")

        horarioSelecionado =
            savedInstanceState.getString("horarioReserva")

        dataExibicao =
            savedInstanceState.getString("dataExibicao")

        reservaConfirmada =
            savedInstanceState.getBoolean(
                "reservaConfirmada"
            )

        atualizarCampoData()

        btnConfirmar.isEnabled = !reservaConfirmada
    }

    /**
     * Limpa data e horário selecionados.
     */
    private fun limparDataHorario() {

        dataSelecionada = null
        horarioSelecionado = null
        dataExibicao = null

        atualizarCampoData()
    }

    /**
     * Atualiza a data exibida no formulário.
     */
    private fun atualizarCampoData() {

        val data = dataExibicao
        val horario = horarioSelecionado

        txtDataHorario.text =
            if (data != null && horario != null) {

                "$data às $horario"

            } else {

                "Selecione a data e o horário"
            }
    }

    /**
     * Controla o carregamento e evita
     * requisições duplicadas.
     */
    private fun definirCarregando(
        ativo: Boolean
    ) {

        carregando = ativo

        txtDataHorario.isEnabled =
            !ativo && !reservaConfirmada

        edtQuantidade.isEnabled =
            !ativo && !reservaConfirmada

        edtNome.isEnabled =
            !ativo && !reservaConfirmada

        edtTelefone.isEnabled =
            !ativo && !reservaConfirmada

        btnConfirmar.isEnabled =
            !ativo && !reservaConfirmada

        btnConfirmar.text =
            if (ativo) {
                "AGUARDE..."
            } else if (reservaConfirmada) {
                "RESERVA REGISTRADA"
            } else {
                "CONTINUAR"
            }
    }

    /**
     * Valida a quantidade de pessoas.
     */
    private fun obterQuantidade(): Int? {

        val quantidade =
            edtQuantidade.text.toString()
                .trim()
                .toIntOrNull()

        if (
            quantidade == null ||
            quantidade !in 1..6
        ) {

            edtQuantidade.error =
                "Informe de 1 a 6 pessoas."

            edtQuantidade.requestFocus()

            MensagemHelper.aviso(
                this,
                "As reservas online permitem de 1 a 6 pessoas."
            )

            return null
        }

        return quantidade
    }

    /**
     * Obtém os limites de datas da API
     * antes de abrir o calendário.
     */
    private fun iniciarSelecaoData() {

        val quantidade = obterQuantidade()
            ?: return

        lifecycleScope.launch {

            definirCarregando(true)

            txtDataHorario.text =
                "Consultando agenda..."

            try {

                val sessao =
                    repository.iniciarSessao()

                abrirCalendario(
                    sessao,
                    quantidade
                )

            } catch (erro: CancellationException) {

                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@ReservaActivity,
                    erro.message
                        ?: "Não foi possível consultar a agenda."
                )

            } finally {

                definirCarregando(false)
                atualizarCampoData()
            }
        }
    }

    /**
     * Calendário personalizado com identidade visual Kiora.
     *
     * Mantém os limites de datas fornecidos pela API
     * e consulta os horários após a confirmação.
     */
    private fun abrirCalendario(
        sessao: SessaoReservaApiResponse,
        quantidade: Int
    ) {

        val corDourada = Color.parseColor("#D9B27A")
        val corVinho = Color.parseColor("#7D1F1F")
        val corFundo = Color.parseColor("#1C1C1C")
        val corBranca = Color.WHITE
        val corDesabilitada = Color.parseColor("#686868")

        // Limites oficiais informados pela API.
        val dataMinima = converterDataApi(sessao.hoje)
        val dataMaxima = converterDataApi(sessao.ultimaData)

        require(!dataMinima.after(dataMaxima)) {
            "O servidor retornou um período de reservas inválido."
        }

        // Mantém a data anteriormente escolhida, se válida.
        var dataEscolhida: Calendar? =
            dataSelecionada?.let {
                converterDataApi(it)
            }?.takeIf {
                !it.before(dataMinima) && !it.after(dataMaxima)
            }

        // Mês inicialmente exibido.
        val mesExibido = (
                dataEscolhida ?: dataMinima
                ).clone() as Calendar

        mesExibido.set(Calendar.DAY_OF_MONTH, 1)

        // Define os primeiros dias dos meses limites.
        val primeiroMes = (
                dataMinima.clone() as Calendar
                ).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }

        val ultimoMes = (
                dataMaxima.clone() as Calendar
                ).apply {
                set(Calendar.DAY_OF_MONTH, 1)
            }

        // Reutiliza o modal visual já existente no Kiora.
        val visual = layoutInflater.inflate(
            R.layout.dialog_confirmar_logout,
            null
        ) as LinearLayout

        val titulo = visual.findViewById<TextView>(
            R.id.txtTituloLogout
        )

        val mensagem = visual.findViewById<TextView>(
            R.id.txtMensagemLogout
        )

        val btnCancelar =
            visual.findViewById<AppCompatButton>(
                R.id.btnCancelarLogout
            )

        val btnConfirmar =
            visual.findViewById<AppCompatButton>(
                R.id.btnConfirmarLogout
            )

        titulo.text = "SELECIONE A DATA"

        mensagem.text =
            "Escolha o dia da sua reserva"

        btnCancelar.text = "VOLTAR"
        btnConfirmar.text = "CONFIRMAR"

        // Container do calendário.
        val calendarioContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(12))
        }

        // Cabeçalho de navegação entre os meses.
        val linhaMes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val btnAnterior = TextView(this).apply {
            text = "‹"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(corDourada)
            contentDescription = "Mês anterior"
            isClickable = true
            isFocusable = true
        }

        val txtMes = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 17f
            setTextColor(corDourada)
            typeface = Typeface.DEFAULT_BOLD
        }

        val btnProximo = TextView(this).apply {
            text = "›"
            textSize = 32f
            gravity = Gravity.CENTER
            setTextColor(corDourada)
            contentDescription = "Próximo mês"
            isClickable = true
            isFocusable = true
        }

        linhaMes.addView(
            btnAnterior,
            LinearLayout.LayoutParams(dp(42), dp(48))
        )

        linhaMes.addView(
            txtMes,
            LinearLayout.LayoutParams(
                0,
                dp(48),
                1f
            )
        )

        linhaMes.addView(
            btnProximo,
            LinearLayout.LayoutParams(dp(42), dp(48))
        )

        calendarioContainer.addView(linhaMes)

        // Cabeçalho dos dias da semana.
        val linhaSemana = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val diasSemana = listOf(
            "D", "S", "T", "Q", "Q", "S", "S"
        )

        diasSemana.forEach { dia ->

            val textoDia = TextView(this).apply {
                text = dia
                textSize = 13f
                gravity = Gravity.CENTER
                setTextColor(corDourada)
                typeface = Typeface.DEFAULT_BOLD
            }

            linhaSemana.addView(
                textoDia,
                LinearLayout.LayoutParams(
                    0,
                    dp(36),
                    1f
                )
            )
        }

        calendarioContainer.addView(linhaSemana)

        // Área onde os dias serão desenhados.
        val gradeDias = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        calendarioContainer.addView(gradeDias)

        // Insere o calendário antes dos botões do modal.
        visual.addView(
            calendarioContainer,
            2,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val dialogo = AlertDialog.Builder(this)
            .setView(visual)
            .create()

        /**
         * Desenha os dias do mês em uma grade
         * de sete colunas, respeitando a API.
         */
        fun atualizarCalendario() {

            gradeDias.removeAllViews()

            val formatoMes = SimpleDateFormat(
                "MMMM 'de' yyyy",
                Locale("pt", "BR")
            ).apply {
                timeZone = fusoRestaurante
            }

            val nomeMes = formatoMes.format(
                mesExibido.time
            )

            txtMes.text = nomeMes.replaceFirstChar {
                it.titlecase(Locale("pt", "BR"))
            }

            // Controla navegação dentro do período permitido.
            val podeVoltar = mesExibido.after(primeiroMes)
            val podeAvancar = mesExibido.before(ultimoMes)

            btnAnterior.isEnabled = podeVoltar
            btnAnterior.alpha = if (podeVoltar) 1f else 0.3f

            btnProximo.isEnabled = podeAvancar
            btnProximo.alpha = if (podeAvancar) 1f else 0.3f

            // Primeiro dia da semana: domingo.
            val deslocamento =
                mesExibido.get(Calendar.DAY_OF_WEEK) - 1

            val totalDias = mesExibido.getActualMaximum(
                Calendar.DAY_OF_MONTH
            )

            val totalLinhas = (
                    deslocamento + totalDias + 6
                    ) / 7

            for (linha in 0 until totalLinhas) {

                val linhaDias = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                for (coluna in 0 until 7) {

                    val numeroDia =
                        linha * 7 + coluna - deslocamento + 1

                    val diaExiste = numeroDia in 1..totalDias

                    val dataDia = if (diaExiste) {
                        (mesExibido.clone() as Calendar).apply {
                            set(Calendar.DAY_OF_MONTH, numeroDia)
                        }
                    } else {
                        null
                    }

                    val permitido = dataDia != null &&
                            !dataDia.before(dataMinima) &&
                            !dataDia.after(dataMaxima)

                    val selecionado = dataDia != null &&
                            dataEscolhida?.let {
                                it.get(Calendar.YEAR) ==
                                        dataDia.get(Calendar.YEAR) &&
                                        it.get(Calendar.MONTH) ==
                                        dataDia.get(Calendar.MONTH) &&
                                        it.get(Calendar.DAY_OF_MONTH) ==
                                        dataDia.get(Calendar.DAY_OF_MONTH)
                            } == true

                    val fundoDia = GradientDrawable().apply {

                        setColor(
                            if (selecionado) {
                                corVinho
                            } else {
                                corFundo
                            }
                        )

                        cornerRadius = dp(9).toFloat()

                        if (selecionado) {
                            setStroke(
                                dp(2),
                                corDourada
                            )
                        }
                    }

                    val botaoDia = TextView(this).apply {

                        text = if (diaExiste) {
                            numeroDia.toString()
                        } else {
                            ""
                        }

                        textSize = 15f

                        gravity = Gravity.CENTER

                        setTextColor(
                            if (permitido) {
                                corBranca
                            } else {
                                corDesabilitada
                            }
                        )

                        background = fundoDia

                        isEnabled = permitido
                        isClickable = permitido
                        isFocusable = permitido

                        if (diaExiste) {
                            contentDescription =
                                "Dia $numeroDia" +
                                        if (selecionado) {
                                            ", selecionado"
                                        } else {
                                            ""
                                        }
                        }
                    }

                    // Seleção de uma data válida.
                    if (permitido && dataDia != null) {

                        botaoDia.setOnClickListener {

                            dataEscolhida =
                                dataDia.clone() as Calendar

                            btnConfirmar.isEnabled = true
                            btnConfirmar.alpha = 1f

                            atualizarCalendario()
                        }
                    }

                    val parametrosDia = LinearLayout.LayoutParams(
                        0,
                        dp(42),
                        1f
                    ).apply {

                        setMargins(
                            dp(2),
                            dp(2),
                            dp(2),
                            dp(2)
                        )
                    }

                    linhaDias.addView(
                        botaoDia,
                        parametrosDia
                    )
                }

                gradeDias.addView(linhaDias)
            }
        }

        // Navegação para o mês anterior.
        btnAnterior.setOnClickListener {

            if (mesExibido.after(primeiroMes)) {
                mesExibido.add(Calendar.MONTH, -1)
                atualizarCalendario()
            }
        }

        // Navegação para o próximo mês.
        btnProximo.setOnClickListener {

            if (mesExibido.before(ultimoMes)) {
                mesExibido.add(Calendar.MONTH, 1)
                atualizarCalendario()
            }
        }

        // Bloqueia a confirmação se nenhuma data foi escolhida.
        btnConfirmar.isEnabled = dataEscolhida != null
        btnConfirmar.alpha =
            if (dataEscolhida != null) 1f else 0.5f

        btnCancelar.setOnClickListener {
            dialogo.dismiss()
        }

        // Confirma a data e consulta os horários reais.
        btnConfirmar.setOnClickListener {

            val selecionada = dataEscolhida
                ?: return@setOnClickListener

            val formatoApi = SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
            ).apply {
                timeZone = fusoRestaurante
            }

            val formatoVisual = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale("pt", "BR")
            ).apply {
                timeZone = fusoRestaurante
            }

            val dataApi = formatoApi.format(
                selecionada.time
            )

            val dataVisual = formatoVisual.format(
                selecionada.time
            )

            dialogo.dismiss()

            // Reutiliza a consulta de horários existente.
            consultarHorarios(
                dataApi,
                dataVisual,
                quantidade
            )
        }

        atualizarCalendario()

        dialogo.show()

        // Preserva o fundo escuro e a borda dourada.
        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogo.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Converte uma data ISO retornada pela API.
     */
    private fun converterDataApi(
        valor: String
    ): Calendar {

        val formato = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).apply {

            isLenient = false
            timeZone = fusoRestaurante
        }

        val data = formato.parse(valor)
            ?: throw IllegalArgumentException(
                "O servidor retornou uma data inválida."
            )

        return Calendar.getInstance(
            fusoRestaurante
        ).apply {

            time = data
        }
    }

    /**
     * Consulta os horários reais disponíveis.
     */
    private fun consultarHorarios(
        data: String,
        dataVisual: String,
        quantidade: Int
    ) {

        lifecycleScope.launch {

            definirCarregando(true)

            txtDataHorario.text =
                "Consultando horários..."

            try {

                val horarios =
                    repository.consultarHorarios(
                        data = data,
                        quantidadePessoas = quantidade
                    )

                val disponiveis = horarios
                    .filter { it.disponivel }
                    .map { it.horario }
                    .distinct()
                    .sorted()

                if (disponiveis.isEmpty()) {

                    MensagemHelper.aviso(
                        this@ReservaActivity,
                        "Não há horários disponíveis para esta data."
                    )

                    return@launch
                }

                // Modal com o padrão visual Kiora.
                abrirModalHorarios(
                    disponiveis,
                    data,
                    dataVisual,
                    quantidade
                )

            } catch (erro: CancellationException) {

                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@ReservaActivity,
                    erro.message
                        ?: "Não foi possível consultar os horários."
                )

            } finally {

                definirCarregando(false)
                atualizarCampoData()
            }
        }
    }

    /**
     * Exibe os horários utilizando o mesmo
     * layout visual dos modais do Kiora.
     */
    private fun abrirModalHorarios(
        horarios: List<String>,
        data: String,
        dataVisual: String,
        quantidade: Int
    ) {

        val visual = layoutInflater.inflate(
            R.layout.dialog_confirmar_logout,
            null
        ) as LinearLayout

        val titulo = visual.findViewById<TextView>(
            R.id.txtTituloLogout
        )

        val mensagem = visual.findViewById<TextView>(
            R.id.txtMensagemLogout
        )

        val btnFechar =
            visual.findViewById<AppCompatButton>(
                R.id.btnCancelarLogout
            )

        val btnExtra =
            visual.findViewById<AppCompatButton>(
                R.id.btnConfirmarLogout
            )

        titulo.text = "SELECIONE O HORÁRIO"

        mensagem.text =
            "Horários disponíveis para $dataVisual"

        btnFechar.text = "FECHAR"

        btnExtra.visibility = View.GONE

        val listaHorarios = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL
        }

        val rolagem = ScrollView(this).apply {

            isFillViewport = false

            addView(listaHorarios)
        }

        visual.addView(
            rolagem,
            2,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(280)
            )
        )

        val dialogo = AlertDialog.Builder(this)
            .setView(visual)
            .create()

        horarios.forEach { horario ->

            val botao = AppCompatButton(this).apply {

                text = horario

                setTextColor(Color.WHITE)

                textSize = 15f

                setBackgroundResource(
                    R.drawable.bg_botao_kiora
                )

                backgroundTintList = null

                isAllCaps = false
            }

            val parametros = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply {

                bottomMargin = dp(8)
            }

            listaHorarios.addView(
                botao,
                parametros
            )

            botao.setOnClickListener {

                val quantidadeAtual =
                    edtQuantidade.text.toString()
                        .trim()
                        .toIntOrNull()

                if (quantidadeAtual != quantidade) {

                    MensagemHelper.aviso(
                        this,
                        "A quantidade foi alterada. Consulte os horários novamente."
                    )

                    dialogo.dismiss()
                    return@setOnClickListener
                }

                dataSelecionada = data
                dataExibicao = dataVisual
                horarioSelecionado = horario

                atualizarCampoData()

                dialogo.dismiss()
            }
        }

        btnFechar.setOnClickListener {
            dialogo.dismiss()
        }

        dialogo.show()

        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        dialogo.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * Converte dp em pixels.
     */
    private fun dp(
        valor: Int
    ): Int {

        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    /**
     * Valida os dados e apresenta o resumo
     * utilizando o modal padronizado.
     */
    private fun validarEConfirmar() {

        val nome = edtNome.text.toString().trim()

        val telefone =
            edtTelefone.text.toString()
                .filter { it in '0'..'9' }

        val quantidade = obterQuantidade()
            ?: return

        val data = dataSelecionada
        val horario = horarioSelecionado

        if (nome.length !in 2..100) {

            edtNome.error =
                "Informe seu nome completo."

            edtNome.requestFocus()
            return
        }

        if (telefone.length !in 10..11) {

            edtTelefone.error =
                "Informe o telefone com DDD."

            edtTelefone.requestFocus()
            return
        }

        if (data == null || horario == null) {

            MensagemHelper.aviso(
                this,
                "Selecione a data e o horário."
            )

            return
        }

        val resumo = buildString {

            append("Nome: $nome")

            append("\nTelefone: $telefone")

            append(
                "\nData: ${dataExibicao ?: data}"
            )

            append("\nHorário: $horario")

            append("\nPessoas: $quantidade")

            append(
                "\n\nA acomodação depende de mesa disponível na chegada."
            )
        }

        // Utiliza o modal centralizado do Kiora.
        MensagemHelper.modalAcao(
            contexto = this,
            titulo = "CONFIRMAR RESERVA",
            mensagem = resumo,
            textoPrincipal = "CONFIRMAR",
            aoPrincipal = {

                enviarReserva(
                    nome = nome,
                    telefone = telefone,
                    data = data,
                    horario = horario,
                    quantidade = quantidade
                )
            },
            textoSecundario = "VOLTAR"
        )
    }

    /**
     * Mantém uma chave para a mesma solicitação,
     * inclusive em caso de falha de conexão.
     */
    private fun obterChaveSolicitacao(
        nome: String,
        telefone: String,
        data: String,
        horario: String,
        quantidade: Int
    ): String {

        val conteudo = listOf(
            nome,
            telefone,
            data,
            horario,
            quantidade.toString()
        ).joinToString("|")

        val assinatura =
            MessageDigest.getInstance("SHA-256")
                .digest(
                    conteudo.toByteArray(
                        Charsets.UTF_8
                    )
                )
                .joinToString("") {

                    "%02x".format(
                        it.toInt() and 0xff
                    )
                }

        val preferencias = getSharedPreferences(
            "kiora_reserva_envio",
            Context.MODE_PRIVATE
        )

        val assinaturaAnterior =
            preferencias.getString(
                "assinatura",
                null
            )

        val chaveAnterior =
            preferencias.getString(
                "chave",
                null
            )

        if (
            assinaturaAnterior == assinatura &&
            !chaveAnterior.isNullOrBlank()
        ) {

            return chaveAnterior
        }

        val novaChave =
            UUID.randomUUID().toString()

        val gravou = preferencias.edit()
            .putString(
                "assinatura",
                assinatura
            )
            .putString(
                "chave",
                novaChave
            )
            .commit()

        if (!gravou) {

            throw IllegalStateException(
                "Não foi possível salvar a identificação da reserva."
            )
        }

        return novaChave
    }

    /**
     * Envia a solicitação ao servidor Kiora.
     */
    private fun enviarReserva(
        nome: String,
        telefone: String,
        data: String,
        horario: String,
        quantidade: Int
    ) {

        lifecycleScope.launch {

            definirCarregando(true)

            try {

                val chave = withContext(
                    Dispatchers.IO
                ) {

                    obterChaveSolicitacao(
                        nome,
                        telefone,
                        data,
                        horario,
                        quantidade
                    )
                }

                val resultado =
                    repository.criarReserva(
                        nome = nome,
                        telefone = telefone,
                        data = data,
                        horario = horario,
                        quantidadePessoas = quantidade,
                        chaveSolicitacao = chave
                    )

                // Guarda o código e a versão
                // para acompanhamento posterior.
                val gravou = withContext(
                    Dispatchers.IO
                ) {

                    getSharedPreferences(
                        "kiora_reservas",
                        Context.MODE_PRIVATE
                    ).edit()
                        .putString(
                            "ultima_reserva_codigo",
                            resultado.codigo
                        )
                        .putString(
                            "ultima_reserva_versao",
                            resultado.versao
                        )
                        .commit()
                }

                if (!gravou) {

                    throw IOException(
                        "A reserva foi recebida pelo servidor, " +
                                "mas não foi possível salvar o código " +
                                "neste dispositivo."
                    )
                }

                // Limpa a identificação da solicitação
                // somente depois da confirmação.
                withContext(Dispatchers.IO) {

                    getSharedPreferences(
                        "kiora_reserva_envio",
                        Context.MODE_PRIVATE
                    ).edit()
                        .clear()
                        .commit()
                }

                reservaConfirmada = true

                exibirConfirmacao(resultado)

            } catch (erro: CancellationException) {

                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@ReservaActivity,
                    erro.message
                        ?: "Não foi possível confirmar a reserva."
                )

            } finally {

                definirCarregando(false)
            }
        }
    }

    /**
     * Modal de confirmação no padrão Kiora.
     *
     * Permite acompanhar a reserva ou copiar
     * seu código de identificação.
     */
    private fun exibirConfirmacao(
        reserva: ReservaApiResponse
    ) {

        val mensagem = buildString {

            append(
                "Sua solicitação foi recebida!"
            )

            append("\n\nCódigo da reserva:")
            append("\n${reserva.codigo}")

            append("\n\nSituação:")
            append("\n${reserva.situacaoTexto}")

            append(
                "\n\nA acomodação depende de mesa disponível na chegada."
            )
        }

        MensagemHelper.modalAcao(
            contexto = this,
            titulo = "RESERVA REGISTRADA",
            mensagem = mensagem,
            textoPrincipal = "ACOMPANHAR",
            aoPrincipal = {

                startActivity(
                    Intent(
                        this,
                        MinhasReservasActivity::class.java
                    )
                )
            },
            textoSecundario = "COPIAR CÓDIGO",
            aoSecundario = {

                val areaTransferencia =
                    getSystemService(
                        CLIPBOARD_SERVICE
                    ) as ClipboardManager

                areaTransferencia.setPrimaryClip(
                    ClipData.newPlainText(
                        "Código da reserva Kiora",
                        reserva.codigo
                    )
                )

                MensagemHelper.sucesso(
                    this,
                    "Código da reserva copiado."
                )
            }
        )
    }
}