
package com.example.appkiora

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast

// Centraliza as mensagens rápidas com a identidade visual do Kiora.
object MensagemHelper {

    private const val DOURADO = "#D9B27A"
    private const val FUNDO = "#1C1C1C"
    private const val BRANCO = "#FFFFFF"
    private const val VERMELHO = "#7D1F1F"

    fun sucesso(contexto: Context, mensagem: String) {
        exibir(contexto, mensagem, DOURADO)
    }

    fun aviso(contexto: Context, mensagem: String) {
        exibir(contexto, mensagem, DOURADO)
    }

    fun erro(contexto: Context, mensagem: String) {
        exibir(contexto, mensagem, VERMELHO)
    }


    private fun exibir(
        contexto: Context,
        mensagem: String,
        corDetalhe: String
    ) {
        val densidade = contexto.resources.displayMetrics.density

        fun dp(valor: Int): Int = (valor * densidade).toInt()

        val fundo = GradientDrawable().apply {
            setColor(Color.rgb(28, 28, 28))
            cornerRadius = dp(14).toFloat()
            setStroke(
                dp(2),
                Color.parseColor(corDetalhe)
            )
        }

        val texto = TextView(contexto).apply {
            text = mensagem
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.create(
                "sans-serif-medium",
                Typeface.NORMAL
            )
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(18),
                dp(14),
                dp(18),
                dp(14)
            )
            background = fundo
            maxLines = 3
            minWidth = dp(220)
        }

        val toast = Toast(contexto.applicationContext).apply {
            duration = Toast.LENGTH_LONG
            view = texto
            setGravity(
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                0,
                dp(100)
            )
        }

        toast.show()
    }

    /**
     * Modal padronizado com a identidade visual Kiora.
     *
     * Reutiliza o layout existente:
     * dialog_confirmar_logout.xml
     */
    fun modalAcao(
        contexto: Context,
        titulo: String,
        mensagem: String,
        textoPrincipal: String,
        aoPrincipal: () -> Unit,
        textoSecundario: String = "FECHAR",
        aoSecundario: (() -> Unit)? = null
    ) {

        val visual = android.view.LayoutInflater
            .from(contexto)
            .inflate(
                R.layout.dialog_confirmar_logout,
                null
            )

        val dialogo =
            androidx.appcompat.app.AlertDialog.Builder(contexto)
                .setView(visual)
                .create()

        val tituloModal = visual.findViewById<TextView>(
            R.id.txtTituloLogout
        )

        val mensagemModal = visual.findViewById<TextView>(
            R.id.txtMensagemLogout
        )

        val btnPrincipal =
            visual.findViewById<
                    androidx.appcompat.widget.AppCompatButton
                    >(R.id.btnConfirmarLogout)

        val btnSecundario =
            visual.findViewById<
                    androidx.appcompat.widget.AppCompatButton
                    >(R.id.btnCancelarLogout)

        tituloModal.text = titulo
        mensagemModal.text = mensagem

        btnPrincipal.text = textoPrincipal
        btnSecundario.text = textoSecundario

        btnPrincipal.setOnClickListener {
            dialogo.dismiss()
            aoPrincipal()
        }

        btnSecundario.setOnClickListener {
            dialogo.dismiss()
            aoSecundario?.invoke()
        }

        dialogo.show()

        dialogo.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )
    }

}
