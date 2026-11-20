package com.example.appkiora

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Intent
import android.net.Uri
import android.widget.ImageButton
import androidx.appcompat.widget.AppCompatButton

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializa a comunicação com a API.
        RetrofitClient.inicializar(applicationContext)

        // Abre o cardápio sem exigir login.
        findViewById<androidx.appcompat.widget.AppCompatButton>(
            R.id.btnCardapio
        ).setOnClickListener {
            startActivity(Intent(this, CardapioActivity::class.java))
        }

        // Abre a tela de reserva sem exigir login.
        findViewById<androidx.appcompat.widget.AppCompatButton>(
            R.id.btnReserva
        ).setOnClickListener {
            startActivity(Intent(this, ReservaActivity::class.java))
        }

        // Acompanhamento de reservas sem exigir login.
        findViewById<AppCompatButton>(
            R.id.btnAcompanharReserva
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MinhasReservasActivity::class.java
                )
            )
        }


        findViewById<ImageButton>(R.id.btnConta).setOnClickListener {

            // Carrega o layout personalizado da janela de conta.
            val dialogView = layoutInflater.inflate(
                R.layout.dialog_minha_conta,
                null
            )

            // Cria a janela com o visual personalizado.
            val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Botão para abrir a tela de login.
            dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(
                R.id.btnEntrarConta
            ).setOnClickListener {
                dialog.dismiss()

                startActivity(
                    Intent(this, LoginActivity::class.java)
                )
            }

            // Botão para abrir a tela de cadastro.
            dialogView.findViewById<androidx.appcompat.widget.AppCompatButton>(
                R.id.btnCadastrarConta
            ).setOnClickListener {
                dialog.dismiss()

                startActivity(
                    Intent(this, CadastroActivity::class.java)
                )
            }

            // Exibe a janela personalizada.
            dialog.show()

            // Remove o fundo padrão do Android nas bordas.
            dialog.window?.setBackgroundDrawableResource(
                android.R.color.transparent
            )
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<ImageButton>(R.id.btnSite).setOnClickListener {
            val url = "https://kiorarestaurante.onrender.com/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnInstagram).setOnClickListener {
            val url = "https://www.instagram.com/kiora_lamen?stkn=N3NhODE1dzZ2bm4y"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnFacebook).setOnClickListener {
            val url = "https://www.facebook.com/share/1EHkGMKTbc/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.btnLocalizacao).setOnClickListener {
            val endereco = Uri.parse("geo:0,0?q=Kiora+Lamen+House")
            val intent = Intent(Intent.ACTION_VIEW, endereco)
            startActivity(intent)
        }
    }
}