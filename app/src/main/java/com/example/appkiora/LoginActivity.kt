
package com.example.appkiora

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.appcompat.widget.AppCompatButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.util.Log

class LoginActivity : AppCompatActivity() {

    // Repositório responsável por realizar o login pela API.
    private val loginRepository = LoginRepository()

    private lateinit var tokenManager: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        tokenManager = TokenManager(applicationContext)

        val campoEmail = findViewById<EditText>(R.id.edtEmail)
        val campoSenha = findViewById<EditText>(R.id.edtSenha)
        val botaoEntrar = findViewById<AppCompatButton>(R.id.btnEntrarLogin)

        // BOTÃO ENTRAR
        botaoEntrar.setOnClickListener {
            val email = campoEmail.text.toString().trim()
            val senha = campoSenha.text.toString()

            // Verifica se o e-mail foi preenchido.
            if (email.isBlank()) {
                campoEmail.error = "Informe seu e-mail."
                campoEmail.requestFocus()
                return@setOnClickListener
            }

            // Verifica se a senha foi preenchida.
            if (senha.isBlank()) {
                campoSenha.error = "Informe sua senha."
                campoSenha.requestFocus()
                return@setOnClickListener
            }

            // Evita cliques repetidos durante a chamada à API.
            botaoEntrar.isEnabled = false

            // Executa a chamada de rede usando uma coroutine.
            lifecycleScope.launch {
                try {
                    val resposta = loginRepository.realizarLogin(
                        email = email,
                        senha = senha
                    )

                    // Confirma que a API retornou sucesso e um token.
                    if (resposta.sucesso && !resposta.token.isNullOrBlank()) {

                        // Salva o token JWT recebido da API.
                        tokenManager.salvarToken(resposta.token!!)

                        MensagemHelper.sucesso(
                            this@LoginActivity,
                            resposta.mensagem
                        )

                        // Abre a área do cliente após o login válido.
                        startActivity(
                            Intent(
                                this@LoginActivity,
                                HomeClienteActivity::class.java
                            )
                        )
                    } else {
                        MensagemHelper.erro(
                            this@LoginActivity,
                            resposta.mensagem
                        )
                    }

                } catch (e: Exception) {
                    Log.e(
                        "KIORA_LOGIN",
                        "Erro ao realizar login pela API",
                        e
                    )

                    MensagemHelper.erro(
                        this@LoginActivity,
                        e.message ?: "Não foi possível conectar à API."
                    )
                } finally {
                    // Reativa o botão mesmo quando ocorre um erro.
                    botaoEntrar.isEnabled = true
                }
            }
        }

        // MOSTRAR OU OCULTAR SENHA
        campoSenha.setOnTouchListener { view, event ->
            if (
                event.action == MotionEvent.ACTION_UP &&
                campoSenha.compoundDrawables[2] != null &&
                event.x >= (
                        campoSenha.width -
                                campoSenha.compoundDrawables[2].bounds.width() -
                                campoSenha.paddingEnd
                        )
            ) {
                view.performClick()

                val senhaOculta =
                    campoSenha.inputType ==
                            (
                                    InputType.TYPE_CLASS_TEXT or
                                            InputType.TYPE_TEXT_VARIATION_PASSWORD
                                    )

                campoSenha.inputType =
                    if (senhaOculta) {
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    } else {
                        InputType.TYPE_CLASS_TEXT or
                                InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }

                campoSenha.setSelection(campoSenha.text.length)
                true
            } else {
                false
            }
        }

        // BOTÃO CADASTRAR
        findViewById<AppCompatButton>(R.id.btnCadastrarLogin)
            .setOnClickListener {
                startActivity(
                    Intent(this, CadastroActivity::class.java)
                )
            }

        // AJUSTE DAS BARRAS DO SISTEMA
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { v, insets ->
            val systemBars =
                insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }
}
