package com.example.appkiora

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException


class RecuperacaoSenhaActivity : AppCompatActivity() {

    private enum class Etapa {
        EMAIL,
        CODIGO,
        NOVA_SENHA
    }

    private val api by lazy {
        RetrofitClient.instancia.create(
            RecuperacaoSenhaApiService::class.java
        )
    }

    private lateinit var txtInstrucao: TextView

    private lateinit var layoutEmail: LinearLayout
    private lateinit var layoutCodigo: LinearLayout
    private lateinit var layoutNovaSenha: LinearLayout

    private lateinit var edtEmail: EditText
    private lateinit var edtCodigo: EditText
    private lateinit var edtNovaSenha: EditText
    private lateinit var edtConfirmarSenha: EditText

    private lateinit var btnSolicitar: AppCompatButton
    private lateinit var btnValidar: AppCompatButton
    private lateinit var btnRedefinir: AppCompatButton
    private lateinit var txtReenviar: TextView

    private var etapaAtual = Etapa.EMAIL
    private var emailRecuperacao = ""
    private var carregando = false

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(
            R.layout.activity_recuperacao_senha
        )

        RetrofitClient.inicializar(
            applicationContext
        )

        configurarInsets()
        inicializarComponentes()

        if (savedInstanceState != null) {

            emailRecuperacao =
                savedInstanceState.getString(
                    "email_recuperacao",
                    ""
                )

            etapaAtual = runCatching {
                Etapa.valueOf(
                    savedInstanceState.getString(
                        "etapa_recuperacao",
                        Etapa.EMAIL.name
                    )
                )
            }.getOrDefault(Etapa.EMAIL)
        }

        configurarBotoes()
        mostrarEtapa(etapaAtual)
    }

    /**
     * Preserva a etapa em caso de
     * recriação da Activity.
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        outState.putString(
            "email_recuperacao",
            emailRecuperacao
        )

        outState.putString(
            "etapa_recuperacao",
            etapaAtual.name
        )

        super.onSaveInstanceState(outState)
    }

    private fun configurarInsets() {

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
    }

    /**
     * Referências aos componentes do XML.
     */
    private fun inicializarComponentes() {

        txtInstrucao = findViewById(
            R.id.txtInstrucaoRecuperacao
        )

        layoutEmail = findViewById(
            R.id.layoutEtapaEmail
        )

        layoutCodigo = findViewById(
            R.id.layoutEtapaCodigo
        )

        layoutNovaSenha = findViewById(
            R.id.layoutEtapaNovaSenha
        )

        edtEmail = findViewById(
            R.id.edtEmailRecuperacao
        )

        edtCodigo = findViewById(
            R.id.edtCodigoRecuperacao
        )

        edtNovaSenha = findViewById(
            R.id.edtNovaSenhaRecuperacao
        )

        edtConfirmarSenha = findViewById(
            R.id.edtConfirmarSenhaRecuperacao
        )

        btnSolicitar = findViewById(
            R.id.btnSolicitarCodigo
        )

        btnValidar = findViewById(
            R.id.btnValidarCodigo
        )

        btnRedefinir = findViewById(
            R.id.btnRedefinirSenha
        )

        txtReenviar = findViewById(
            R.id.txtReenviarCodigo
        )
    }

    /**
     * Configura os eventos da interface.
     */
    private fun configurarBotoes() {

        btnSolicitar.setOnClickListener {
            solicitarCodigo()
        }

        btnValidar.setOnClickListener {
            validarCodigo()
        }

        btnRedefinir.setOnClickListener {
            redefinirSenha()
        }

        txtReenviar.setOnClickListener {
            solicitarCodigo(reenviar = true)
        }

        findViewById<TextView>(
            R.id.txtVoltarLoginRecuperacao
        ).setOnClickListener {
            finish()
        }
    }

    /**
     * Controla qual etapa é apresentada.
     */
    private fun mostrarEtapa(
        etapa: Etapa
    ) {

        etapaAtual = etapa

        layoutEmail.visibility =
            if (etapa == Etapa.EMAIL) {
                View.VISIBLE
            } else {
                View.GONE
            }

        layoutCodigo.visibility =
            if (etapa == Etapa.CODIGO) {
                View.VISIBLE
            } else {
                View.GONE
            }

        layoutNovaSenha.visibility =
            if (etapa == Etapa.NOVA_SENHA) {
                View.VISIBLE
            } else {
                View.GONE
            }

        txtInstrucao.text = when (etapa) {

            Etapa.EMAIL ->
                "Informe o e-mail cadastrado " +
                        "para receber um código de recuperação."

            Etapa.CODIGO ->
                "Se o e-mail estiver cadastrado, " +
                        "você receberá um código de 6 dígitos.\n\n" +
                        "Digite o código recebido para continuar."

            Etapa.NOVA_SENHA ->
                "Crie uma nova senha com pelo menos " +
                        "8 caracteres, incluindo uma letra " +
                        "e um número."
        }
    }

    /**
     * Ativa ou bloqueia as ações enquanto
     * uma requisição está em andamento.
     */
    private fun definirCarregando(
        ativo: Boolean
    ) {

        carregando = ativo

        btnSolicitar.isEnabled = !ativo
        btnValidar.isEnabled = !ativo
        btnRedefinir.isEnabled = !ativo

        txtReenviar.isEnabled = !ativo

        edtEmail.isEnabled = !ativo
        edtCodigo.isEnabled = !ativo
        edtNovaSenha.isEnabled = !ativo
        edtConfirmarSenha.isEnabled = !ativo

        btnSolicitar.text =
            if (ativo && etapaAtual == Etapa.EMAIL) {
                "AGUARDE..."
            } else {
                "ENVIAR CÓDIGO"
            }

        btnValidar.text =
            if (ativo && etapaAtual == Etapa.CODIGO) {
                "VALIDANDO..."
            } else {
                "VALIDAR CÓDIGO"
            }

        btnRedefinir.text =
            if (ativo && etapaAtual == Etapa.NOVA_SENHA) {
                "AGUARDE..."
            } else {
                "REDEFINIR SENHA"
            }
    }

    /**
     * Validação local do e-mail.
     */
    private fun emailValido(
        email: String
    ): Boolean {

        return email.isNotBlank() &&
                email.length <= 150 &&
                Patterns.EMAIL_ADDRESS
                    .matcher(email)
                    .matches()
    }

    /**
     * Validação local do código.
     */
    private fun codigoValido(
        codigo: String
    ): Boolean {

        return codigo.length == 6 &&
                codigo.all { it in '0'..'9' }
    }

    /**
     * Segue a regra de senha do backend:
     * mínimo 8 caracteres, uma letra e um
     * número de 0 a 9.
     */
    private fun senhaValida(
        senha: String
    ): Boolean {

        return senha.length >= 8 &&
                senha.any { it.isLetter() } &&
                senha.any { it in '0'..'9' }
    }

    /**
     * Executa uma chamada à API sem
     * bloquear a interface.
     */
    private fun executarOperacao(
        operacao: suspend () ->
        Response<RecuperacaoSenhaApiResponse>,

        aoSucesso: (
            RecuperacaoSenhaApiResponse
        ) -> Unit
    ) {

        if (carregando) {
            return
        }

        definirCarregando(true)

        lifecycleScope.launch {

            try {

                val resposta = operacao()

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
                        "O servidor retornou uma resposta vazia."
                    )

                if (!resultado.sucesso) {

                    throw IllegalStateException(
                        resultado.mensagem.ifBlank {
                            "Não foi possível concluir a operação."
                        }
                    )
                }

                aoSucesso(resultado)

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@RecuperacaoSenhaActivity,
                    erro.message
                        ?: "Não foi possível comunicar com o servidor."
                )

            } finally {

                definirCarregando(false)
            }
        }
    }

    /**
     * ETAPA 1:
     * Solicita o código de recuperação.
     *
     * A mesma função também permite
     * solicitar um novo código.
     */
    private fun solicitarCodigo(
        reenviar: Boolean = false
    ) {

        if (carregando) {
            return
        }

        val email = if (reenviar) {
            emailRecuperacao
        } else {
            edtEmail.text.toString().trim()
        }

        if (!emailValido(email)) {

            edtEmail.error =
                "Informe um e-mail válido."

            if (!reenviar) {
                edtEmail.requestFocus()
            }

            MensagemHelper.aviso(
                this,
                "Informe um endereço de e-mail válido."
            )

            return
        }

        executarOperacao(
            operacao = {

                api.solicitarCodigo(
                    SolicitarCodigoRecuperacaoRequest(
                        email = email
                    )
                )
            },

            aoSucesso = {

                emailRecuperacao = email

                // Evita utilizar um código antigo
                // depois de solicitar um novo.
                edtCodigo.text.clear()
                edtNovaSenha.text.clear()
                edtConfirmarSenha.text.clear()

                mostrarEtapa(Etapa.CODIGO)

                MensagemHelper.aviso(
                    this,
                    "Se o e-mail estiver cadastrado, " +
                            "você receberá um código de recuperação."
                )
            }
        )
    }

    /**
     * ETAPA 2:
     * Valida o código informado.
     */
    private fun validarCodigo() {

        if (carregando) {
            return
        }

        val codigo = edtCodigo.text
            .toString()
            .trim()

        if (!codigoValido(codigo)) {

            edtCodigo.error =
                "Informe os 6 dígitos."

            edtCodigo.requestFocus()

            MensagemHelper.aviso(
                this,
                "O código deve conter 6 números."
            )

            return
        }

        if (!emailValido(emailRecuperacao)) {

            mostrarEtapa(Etapa.EMAIL)

            MensagemHelper.aviso(
                this,
                "Informe novamente seu e-mail."
            )

            return
        }

        executarOperacao(
            operacao = {

                api.validarCodigo(
                    ValidarCodigoRecuperacaoRequest(
                        email = emailRecuperacao,
                        codigo = codigo
                    )
                )
            },

            aoSucesso = {

                mostrarEtapa(Etapa.NOVA_SENHA)

                MensagemHelper.sucesso(
                    this,
                    "Código validado. " +
                            "Agora escolha sua nova senha."
                )
            }
        )
    }

    /**
     * ETAPA 3:
     * Envia a nova senha ao backend.
     */
    private fun redefinirSenha() {

        if (carregando) {
            return
        }

        val codigo = edtCodigo.text
            .toString()
            .trim()

        val novaSenha =
            edtNovaSenha.text.toString()

        val confirmarSenha =
            edtConfirmarSenha.text.toString()

        if (
            !emailValido(emailRecuperacao) ||
            !codigoValido(codigo)
        ) {

            MensagemHelper.aviso(
                this,
                "A recuperação precisa ser iniciada novamente."
            )

            mostrarEtapa(Etapa.EMAIL)
            return
        }

        if (!senhaValida(novaSenha)) {

            edtNovaSenha.error =
                "Use ao menos 8 caracteres, " +
                        "uma letra e um número."

            edtNovaSenha.requestFocus()

            MensagemHelper.aviso(
                this,
                "A senha deve conter pelo menos " +
                        "8 caracteres, uma letra e um número."
            )

            return
        }

        if (novaSenha != confirmarSenha) {

            edtConfirmarSenha.error =
                "As senhas não coincidem."

            edtConfirmarSenha.requestFocus()

            MensagemHelper.aviso(
                this,
                "A confirmação deve ser igual à nova senha."
            )

            return
        }

        executarOperacao(
            operacao = {

                api.redefinirSenha(
                    RedefinirSenhaRecuperacaoRequest(
                        email = emailRecuperacao,
                        codigo = codigo,
                        novaSenha = novaSenha,
                        confirmarNovaSenha = confirmarSenha
                    )
                )
            },

            aoSucesso = {

                // Limpa os dados sensíveis da tela.
                edtCodigo.text.clear()
                edtNovaSenha.text.clear()
                edtConfirmarSenha.text.clear()

                MensagemHelper.modalAcao(
                    contexto =
                        this@RecuperacaoSenhaActivity,

                    titulo = "SENHA REDEFINIDA",

                    mensagem =
                        "Sua senha foi redefinida " +
                                "com sucesso.\n\n" +
                                "Você já pode acessar sua " +
                                "conta com a nova senha.",

                    textoPrincipal = "IR PARA LOGIN",

                    aoPrincipal = {
                        finish()
                    },

                    textoSecundario = "FECHAR",

                    aoSecundario = {
                        finish()
                    }
                )
            }
        )
    }

    /**
     * Interpreta erros HTTP sem mostrar
     * dados técnicos ou informações sensíveis.
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
                // Usa o tratamento padrão abaixo.
            }
        }

        return when (codigoHttp) {

            400 ->
                "Confira os dados informados e tente novamente."

            401, 403 ->
                "Não foi possível validar a solicitação."

            404 ->
                "A recuperação de senha ainda não está " +
                        "disponível na API do Kiora."

            429 ->
                "Muitas tentativas de recuperação. " +
                        "Aguarde antes de tentar novamente."

            in 500..599 ->
                "O servidor apresentou um erro. " +
                        "Tente novamente mais tarde."

            else ->
                "Não foi possível concluir a solicitação. " +
                        "HTTP $codigoHttp."
        }
    }
}