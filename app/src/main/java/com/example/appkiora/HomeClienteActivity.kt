package com.example.appkiora

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.ImageButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.widget.AppCompatButton
import kotlinx.coroutines.launch

class HomeClienteActivity : AppCompatActivity() {

    private val loginRepository = LoginRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(
            R.layout.activity_home_cliente
        )

        // Consulta o perfil do usuário autenticado.
        lifecycleScope.launch {
            try {

                val perfil =
                    loginRepository.buscarPerfil()

                val dadosUsuario = perfil.usuario

                val nome = dadosUsuario
                    ?.get("nome")
                    ?.asString
                    ?.substringBefore(" ")
                    ?: "Cliente"

                findViewById<TextView>(
                    R.id.txtSaudacao
                ).text = "Olá, $nome! 👋"

            } catch (e: Exception) {

                MensagemHelper.erro(
                    this@HomeClienteActivity,
                    e.message
                        ?: "Não foi possível carregar seu perfil."
                )
            }
        }

        /**
         * NOVO: FAZER RESERVA
         *
         * Utiliza a ReservaActivity existente.
         *
         * O RetrofitClient já acrescenta
         * o token JWT às requisições da API
         * quando o cliente está logado.
         */
        findViewById<AppCompatButton>(
            R.id.btnFazerReservaCliente
        ).setOnClickListener {

            val intent = Intent(
                this,
                ReservaActivity::class.java
            )

            startActivity(intent)
        }

        /**
         * MINHAS RESERVAS
         *
         * Abre a tela de acompanhamento
         * e histórico das reservas.
         */
        findViewById<AppCompatButton>(
            R.id.btnMinhasReservas
        ).setOnClickListener {

            val intent = Intent(
                this,
                MinhasReservasActivity::class.java
            )

            startActivity(intent)
        }

        /**
         * MEU PERFIL
         *
         * Mantém acesso à edição de perfil
         * e alteração de senha.
         */
        findViewById<AppCompatButton>(
            R.id.btnMeuPerfil
        ).setOnClickListener {

            val intent = Intent(
                this,
                MeuPerfilActivity::class.java
            )

            startActivity(intent)
        }

        /**
         * SAIR DA CONTA
         */
        findViewById<ImageButton>(
            R.id.btnSair
        ).setOnClickListener {

            confirmarLogout()
        }

        // Respeita as barras do Android.
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
     * Preserva o modal original do Kiora
     * para confirmação de logout.
     */
    private fun confirmarLogout() {

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

        btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirmar.setOnClickListener {

            // Remove o JWT salvo no dispositivo.
            TokenManager(
                applicationContext
            ).limparToken()

            dialog.dismiss()

            MensagemHelper.sucesso(
                this@HomeClienteActivity,
                "Você saiu da sua conta."
            )

            val intent = Intent(
                this@HomeClienteActivity,
                LoginActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            finish()
        }

        dialog.show()

        dialog.window
            ?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
    }
}