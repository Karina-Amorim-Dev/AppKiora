package com.example.appkiora

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class CadastroActivity : AppCompatActivity() {

    private val cadastroRepository = CadastroRepository()

    // Cores utilizadas no projeto Kiora.
    private val corDourada = Color.parseColor("#D9B27A")
    private val corVermelha = Color.parseColor("#7D1F1F")
    private val corFundo = Color.parseColor("#1C1C1C")

    // Controle do cadastro.
    private var enviandoCadastro = false
    private var cadastroConcluido = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_cadastro)

        RetrofitClient.inicializar(applicationContext)

        // Termos de Uso.
        findViewById<TextView>(R.id.txtTermosUso)
            .setOnClickListener {

                val endereco =
                    "https://kiorarestaurante.onrender.com/Home/TermosUso"

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(endereco)
                    )
                )
            }

        // Política de Privacidade.
        findViewById<TextView>(R.id.txtPoliticaPrivacidade)
            .setOnClickListener {

                val endereco =
                    "https://kiorarestaurante.onrender.com/Home/PoliticaPrivacidade"

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(endereco)
                    )
                )
            }

        // Campos do formulário.
        val edtNome = findViewById<EditText>(
            R.id.edtNome
        )

        val edtEmail = findViewById<EditText>(
            R.id.edtEmailCadastro
        )

        val edtSenha = findViewById<EditText>(
            R.id.edtSenhaCadastro
        )

        val edtConfirmarSenha = findViewById<EditText>(
            R.id.edtConfirmarSenha
        )

        val chkAceiteTermos = findViewById<CheckBox>(
            R.id.chkAceiteTermos
        )

        val btnCadastrar = findViewById<AppCompatButton>(
            R.id.btnCadastrar
        )

        // Mostrar ou ocultar senha.
        configurarVisibilidadeSenha(edtSenha)

        // Mostrar ou ocultar confirmação de senha.
        configurarVisibilidadeSenha(edtConfirmarSenha)

        // Botão CADASTRAR.
        btnCadastrar.setOnClickListener {

            if (enviandoCadastro || cadastroConcluido) {
                return@setOnClickListener
            }

            val nome = edtNome.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val senha = edtSenha.text.toString()

            val confirmarSenha =
                edtConfirmarSenha.text.toString()

            // Validação do nome.
            if (nome.isBlank() || nome.length > 100) {

                edtNome.error =
                    "Informe um nome de até 100 caracteres."

                edtNome.requestFocus()
                return@setOnClickListener
            }

            // Validação do e-mail.
            if (
                email.isBlank() ||
                email.length > 150 ||
                !Patterns.EMAIL_ADDRESS.matcher(email).matches()
            ) {

                edtEmail.error = "Informe um e-mail válido."
                edtEmail.requestFocus()
                return@setOnClickListener
            }

            // Validação da senha.
            if (senha.length < 8) {

                edtSenha.error =
                    "A senha deve ter pelo menos 8 caracteres."

                edtSenha.requestFocus()
                return@setOnClickListener
            }

            if (
                !senha.any { it.isLetter() } ||
                !senha.any { it in '0'..'9' }
            ) {

                edtSenha.error =
                    "A senha deve conter letras e números."

                edtSenha.requestFocus()
                return@setOnClickListener
            }

            // Confirmação de senha.
            if (senha != confirmarSenha) {

                edtConfirmarSenha.error =
                    "As senhas não coincidem."

                edtConfirmarSenha.requestFocus()
                return@setOnClickListener
            }

            // Aceite dos termos.
            if (!chkAceiteTermos.isChecked) {

                chkAceiteTermos.error =
                    "É necessário aceitar os termos."

                MensagemHelper.aviso(
                    this@CadastroActivity,
                    "Aceite os Termos de Uso e a Política de Privacidade."
                )

                return@setOnClickListener
            }

            // Prepara a requisição.
            val dadosCadastro = try {

                cadastroRepository.prepararCadastro(
                    nome = nome,
                    email = email,
                    senha = senha,
                    confirmarSenha = confirmarSenha,
                    aceiteTermos = chkAceiteTermos.isChecked
                )

            } catch (erro: IllegalArgumentException) {

                MensagemHelper.erro(
                    this@CadastroActivity,
                    erro.message ?: "Dados do cadastro inválidos."
                )

                return@setOnClickListener
            }

            // Bloqueia novos envios.
            enviandoCadastro = true
            btnCadastrar.isEnabled = false
            btnCadastrar.text = "CADASTRANDO..."

            // Comunicação com a API.
            lifecycleScope.launch {

                try {

                    val resultado = cadastroRepository.cadastrar(
                        dadosCadastro
                    )

                    when (resultado) {

                        is ResultadoCadastro.Sucesso -> {

                            cadastroConcluido = true

                            // Modal padronizado Kiora.
                            exibirModalCadastroSucesso(
                                resultado.mensagem
                            )
                        }

                        is ResultadoCadastro.EmailExistente -> {

                            edtEmail.error = resultado.mensagem
                            edtEmail.requestFocus()

                            MensagemHelper.aviso(
                                this@CadastroActivity,
                                resultado.mensagem
                            )
                        }

                        is ResultadoCadastro.DadosInvalidos -> {

                            MensagemHelper.erro(
                                this@CadastroActivity,
                                resultado.mensagem
                            )
                        }

                        is ResultadoCadastro.ErroConexao -> {

                            MensagemHelper.erro(
                                this@CadastroActivity,
                                resultado.mensagem
                            )
                        }

                        is ResultadoCadastro.ErroGeral -> {

                            MensagemHelper.erro(
                                this@CadastroActivity,
                                resultado.mensagem
                            )
                        }
                    }

                } catch (erro: CancellationException) {

                    throw erro

                } catch (_: Exception) {

                    MensagemHelper.erro(
                        this@CadastroActivity,
                        "Ocorreu um erro inesperado. " +
                                "Não foi possível confirmar o cadastro."
                    )

                } finally {

                    enviandoCadastro = false

                    btnCadastrar.isEnabled = !cadastroConcluido

                    // Evita deixar CADASTRANDO...
                    // aparecendo atrás do modal.
                    btnCadastrar.text =
                        if (cadastroConcluido) {
                            "CADASTRADO"
                        } else {
                            "CADASTRAR"
                        }
                }
            }
        }

        // Acesso à tela de login.
        findViewById<TextView>(R.id.txtFazerLogin)
            .setOnClickListener {

                startActivity(
                    Intent(
                        this,
                        LoginActivity::class.java
                    )
                )
            }

        // Ajuste das barras do sistema.
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    /**
     * Mantém o comportamento de mostrar e ocultar
     * senhas utilizando o ícone original do campo.
     */
    private fun configurarVisibilidadeSenha(
        campo: EditText
    ) {

        campo.setOnTouchListener { view, event ->

            val icone = campo.compoundDrawables[2]

            if (
                event.action == MotionEvent.ACTION_UP &&
                icone != null &&
                event.x >= (
                        campo.width -
                                icone.bounds.width() -
                                campo.paddingEnd
                        )
            ) {

                view.performClick()

                val senhaOculta =
                    campo.inputType == (
                            InputType.TYPE_CLASS_TEXT or
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD
                            )

                campo.inputType =
                    if (senhaOculta) {

                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

                    } else {

                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                campo.setSelection(campo.text.length)

                true

            } else {
                false
            }
        }
    }

    /**
     * Converte dp em pixels.
     */
    private fun dp(valor: Int): Int {
        return (valor * resources.displayMetrics.density).toInt()
    }

    /**
     * Modal de sucesso com a identidade visual Kiora.
     *
     * Fundo escuro, borda dourada, título dourado
     * e botão vermelho.
     */
    private fun exibirModalCadastroSucesso(
        mensagem: String
    ) {

        // Fundo do modal.
        val fundoModal = GradientDrawable().apply {

            setColor(corFundo)

            cornerRadius = dp(16).toFloat()

            setStroke(
                dp(2),
                corDourada
            )
        }

        // Container principal.
        val conteudo = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            gravity = Gravity.CENTER_HORIZONTAL

            setPadding(
                dp(24),
                dp(28),
                dp(24),
                dp(24)
            )

            background = fundoModal
        }

        // Título.
        val titulo = TextView(this).apply {

            text = "CADASTRO REALIZADO!"

            setTextColor(corDourada)

            textSize = 20f

            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.BOLD
            )

            gravity = Gravity.CENTER
        }

        conteudo.addView(
            titulo,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Mensagem de sucesso.
        val textoMensagem = TextView(this).apply {

            text = mensagem.ifBlank {
                "Usuário cadastrado com sucesso."
            }

            setTextColor(Color.WHITE)

            textSize = 16f

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(20),
                0,
                0
            )
        }

        conteudo.addView(
            textoMensagem,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Orientação após o cadastro.
        val textoOrientacao = TextView(this).apply {

            text = "Agora você pode entrar com seu e-mail e senha."

            setTextColor(Color.WHITE)

            textSize = 15f

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(14),
                0,
                0
            )
        }

        conteudo.addView(
            textoOrientacao,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Fundo do botão.
        val fundoBotao = GradientDrawable().apply {

            setColor(corVermelha)

            cornerRadius = dp(14).toFloat()

            setStroke(
                dp(2),
                corDourada
            )
        }

        // Botão de encaminhamento.
        val btnLogin = AppCompatButton(this).apply {

            text = "IR PARA LOGIN"

            isAllCaps = false

            setTextColor(Color.WHITE)

            textSize = 15f

            typeface = Typeface.DEFAULT_BOLD

            background = fundoBotao

            gravity = Gravity.CENTER
        }

        val parametrosBotao = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(54)
        ).apply {

            topMargin = dp(26)
        }

        conteudo.addView(
            btnLogin,
            parametrosBotao
        )

        // Cria o modal personalizado.
        val dialogo = AlertDialog.Builder(this)
            .setView(conteudo)
            .setCancelable(false)
            .create()

        // Ação do botão.
        btnLogin.setOnClickListener {

            dialogo.dismiss()

            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )

            finish()
        }

        // Exibe o modal.
        dialogo.show()

        // Remove o fundo padrão branco do Android.
        dialogo.window?.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        // Ajusta a largura para diferentes telas.
        val larguraTela = resources.displayMetrics.widthPixels

        dialogo.window?.setLayout(
            (larguraTela * 0.88f).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}