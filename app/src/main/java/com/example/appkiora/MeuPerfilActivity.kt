package com.example.appkiora

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageButton
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

class MeuPerfilActivity : AppCompatActivity() {

    // Repositório responsável pelas chamadas à API do Kiora.
    private val loginRepository = LoginRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_meu_perfil)

        // Ajusta a tela para respeitar as barras do sistema.
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

        // Volta para a tela anterior.
        findViewById<AppCompatButton>(
            R.id.btnVoltarPerfil
        ).setOnClickListener {
            finish()
        }

        // Abre o modal personalizado de saída.
        findViewById<ImageButton>(
            R.id.btnSair
        ).setOnClickListener {
            abrirModalLogout()
        }

        // Abre o modal de edição do perfil.
        findViewById<AppCompatButton>(
            R.id.btnEditarPerfil
        ).setOnClickListener {
            abrirModalEditarPerfil()
        }

        // Abre o modal de alteração de senha.
        findViewById<AppCompatButton>(
            R.id.btnAlterarSenha
        ).setOnClickListener {
            abrirModalAlterarSenha()
        }

        // Carrega os dados do usuário autenticado.
        carregarPerfil()
    }

    /**
     * Exibe o modal personalizado de confirmação de saída.
     */
    private fun abrirModalLogout() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_confirmar_logout,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val btnCancelar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnCancelarLogout
            )

        val btnConfirmar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnConfirmarLogout
            )

        // Fecha o modal sem sair da conta.
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Remove o token e retorna ao login.
        btnConfirmar.setOnClickListener {

            TokenManager(this).limparToken()

            dialog.dismiss()

            val intent = Intent(
                this,
                LoginActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )
    }

    /**
     * Consulta o perfil pela API.
     */
    private fun carregarPerfil() {

        lifecycleScope.launch {

            try {

                val resposta = loginRepository.buscarPerfil()
                val usuario = resposta.usuario

                if (usuario == null) {

                    MensagemHelper.erro(
                        this@MeuPerfilActivity,
                        "Não foi possível encontrar os dados do perfil."
                    )

                    return@launch
                }

                findViewById<TextView>(
                    R.id.txtNomePerfil
                ).text = usuario.get("nome")?.asString
                    ?: "Não informado"

                findViewById<TextView>(
                    R.id.txtEmailPerfil
                ).text = usuario.get("email")?.asString
                    ?: "Não informado"

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                MensagemHelper.erro(
                    this@MeuPerfilActivity,
                    erro.message
                        ?: "Não foi possível carregar seu perfil."
                )
            }
        }
    }

    /**
     * Abre o modal para editar nome e e-mail.
     */
    private fun abrirModalEditarPerfil() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_editar_perfil,
            null
        )

        val campoNome =
            dialogView.findViewById<EditText>(
                R.id.edtNomePerfil
            )

        val campoEmail =
            dialogView.findViewById<EditText>(
                R.id.edtEmailPerfil
            )

        val btnSalvar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnSalvarEdicaoPerfil
            )

        val btnCancelar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnCancelarEdicaoPerfil
            )

        // Preenche os campos com os dados atuais.
        campoNome.setText(
            findViewById<TextView>(
                R.id.txtNomePerfil
            ).text.toString()
        )

        campoEmail.setText(
            findViewById<TextView>(
                R.id.txtEmailPerfil
            ).text.toString()
        )

        // Cria e exibe o modal personalizado.
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Fecha sem salvar alterações.
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Valida e envia as alterações à API.
        btnSalvar.setOnClickListener {

            val nome = campoNome.text.toString().trim()
            val email = campoEmail.text.toString().trim()

            if (nome.isBlank() || email.isBlank()) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "Preencha o nome e o e-mail."
                )

                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "Digite um e-mail válido."
                )

                return@setOnClickListener
            }

            lifecycleScope.launch {

                btnSalvar.isEnabled = false

                try {

                    val resposta = loginRepository.atualizarPerfil(
                        nome = nome,
                        email = email
                    )

                    // Atualiza os campos com os dados da API.
                    val usuario = resposta.usuario

                    findViewById<TextView>(
                        R.id.txtNomePerfil
                    ).text = usuario?.get("nome")?.asString
                        ?: nome

                    findViewById<TextView>(
                        R.id.txtEmailPerfil
                    ).text = usuario?.get("email")?.asString
                        ?: email

                    dialog.dismiss()

                    MensagemHelper.sucesso(
                        this@MeuPerfilActivity,
                        "Perfil atualizado com sucesso!"
                    )

                } catch (erro: CancellationException) {
                    throw erro

                } catch (erro: Exception) {

                    MensagemHelper.erro(
                        this@MeuPerfilActivity,
                        erro.message
                            ?: "Não foi possível atualizar o perfil."
                    )

                } finally {
                    btnSalvar.isEnabled = true
                }
            }
        }
    }

    /**
     * Alterna entre mostrar e ocultar a senha.
     * Preserva o comportamento dos ícones existentes.
     */
    private fun configurarOlhoSenha(
        campoSenha: EditText,
        botaoOlho: ImageButton
    ) {

        var senhaVisivel = false

        botaoOlho.setOnClickListener {

            val posicaoCursor = campoSenha.selectionStart

            senhaVisivel = !senhaVisivel

            campoSenha.inputType = if (senhaVisivel) {

                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

            } else {

                InputType.TYPE_CLASS_TEXT or
                        InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            campoSenha.setSelection(
                posicaoCursor.coerceIn(
                    0,
                    campoSenha.text.length
                )
            )

            botaoOlho.contentDescription =
                if (senhaVisivel) {
                    "Ocultar senha"
                } else {
                    "Mostrar senha"
                }
        }
    }

    /**
     * Abre o modal de alteração de senha.
     *
     * Após a confirmação da API, armazena o
     * novo JWT para manter o usuário autenticado.
     */
    private fun abrirModalAlterarSenha() {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_alterar_senha,
            null
        )

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Campos originais do modal.
        val campoSenhaAtual =
            dialogView.findViewById<EditText>(
                R.id.edtSenhaAtual
            )

        val campoNovaSenha =
            dialogView.findViewById<EditText>(
                R.id.edtNovaSenha
            )

        val campoConfirmarSenha =
            dialogView.findViewById<EditText>(
                R.id.edtConfirmarNovaSenha
            )

        // Botões originais.
        val btnCancelar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnCancelarAlterarSenha
            )

        val btnSalvar =
            dialogView.findViewById<AppCompatButton>(
                R.id.btnSalvarNovaSenha
            )

        // Botões de visualização de senha.
        val btnOlhoSenhaAtual =
            dialogView.findViewById<ImageButton>(
                R.id.btnMostrarSenhaAtual
            )

        val btnOlhoNovaSenha =
            dialogView.findViewById<ImageButton>(
                R.id.btnMostrarNovaSenha
            )

        val btnOlhoConfirmarSenha =
            dialogView.findViewById<ImageButton>(
                R.id.btnMostrarConfirmarSenha
            )

        // Mantém o comportamento dos três olhos.
        configurarOlhoSenha(
            campoSenhaAtual,
            btnOlhoSenhaAtual
        )

        configurarOlhoSenha(
            campoNovaSenha,
            btnOlhoNovaSenha
        )

        configurarOlhoSenha(
            campoConfirmarSenha,
            btnOlhoConfirmarSenha
        )

        // Fecha sem alterar a senha.
        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        // Salva a nova senha.
        btnSalvar.setOnClickListener {

            val senhaAtual =
                campoSenhaAtual.text.toString()

            val novaSenha =
                campoNovaSenha.text.toString()

            val confirmarSenha =
                campoConfirmarSenha.text.toString()

            // Valida os campos obrigatórios.
            if (
                senhaAtual.isBlank() ||
                novaSenha.isBlank() ||
                confirmarSenha.isBlank()
            ) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "Preencha todos os campos."
                )

                return@setOnClickListener
            }

            // Confirmação da nova senha.
            if (novaSenha != confirmarSenha) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "A confirmação não corresponde à nova senha."
                )

                return@setOnClickListener
            }

            // Validação mínima da senha.
            if (novaSenha.length < 8) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "A nova senha deve ter pelo menos 8 caracteres."
                )

                return@setOnClickListener
            }

            if (
                !novaSenha.any { it.isLetter() } ||
                !novaSenha.any { it in '0'..'9' }
            ) {

                MensagemHelper.aviso(
                    this@MeuPerfilActivity,
                    "A nova senha deve conter letras e números."
                )

                return@setOnClickListener
            }

            lifecycleScope.launch {

                btnSalvar.isEnabled = false
                btnCancelar.isEnabled = false
                btnSalvar.text = "SALVANDO..."

                try {

                    // Altera a senha no servidor Kiora.
                    val resposta = loginRepository.alterarSenha(
                        senhaAtual = senhaAtual,
                        novaSenha = novaSenha,
                        confirmarNovaSenha = confirmarSenha
                    )

                    // Recupera o novo JWT.
                    val novoToken = resposta.token
                        ?.takeIf { it.isNotBlank() }

                    // Fecha o modal somente após sucesso.
                    dialog.dismiss()

                    if (novoToken != null) {

                        // Substitui o JWT anterior.
                        TokenManager(
                            this@MeuPerfilActivity
                        ).salvarToken(novoToken)

                        // Mantém o usuário autenticado.
                        MensagemHelper.sucesso(
                            this@MeuPerfilActivity,
                            resposta.mensagem.ifBlank {
                                "Senha alterada com sucesso!"
                            }
                        )

                    } else {

                        // Sem novo JWT não é seguro manter
                        // o token anterior, pois a sessão
                        // pode ter sido invalidada.
                        TokenManager(
                            this@MeuPerfilActivity
                        ).limparToken()

                        MensagemHelper.aviso(
                            this@MeuPerfilActivity,
                            "Senha alterada. Faça login novamente."
                        )

                        val intent = Intent(
                            this@MeuPerfilActivity,
                            LoginActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }

                        startActivity(intent)
                        finish()
                    }

                } catch (erro: CancellationException) {
                    throw erro

                } catch (erro: Exception) {

                    MensagemHelper.erro(
                        this@MeuPerfilActivity,
                        erro.message
                            ?: "Não foi possível alterar a senha."
                    )

                } finally {

                    btnSalvar.isEnabled = true
                    btnCancelar.isEnabled = true
                    btnSalvar.text = "SALVAR"
                }
            }
        }

        // Exibe o modal no padrão visual original.
        dialog.show()

        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )
    }
}