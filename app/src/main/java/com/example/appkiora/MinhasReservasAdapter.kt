package com.example.appkiora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class MinhasReservasAdapter(
    private var reservas: List<ReservaApiResponse>,
    private val onCancelarClick:
    ((ReservaApiResponse) -> Unit)? = null
) : RecyclerView.Adapter<
        MinhasReservasAdapter.ReservaViewHolder
        >() {

    class ReservaViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val txtDataHora: TextView =
            itemView.findViewById(
                R.id.txtDataHoraItemReserva
            )

        val txtQuantidade: TextView =
            itemView.findViewById(
                R.id.txtQuantidadeItemReserva
            )

        val txtObservacoes: TextView =
            itemView.findViewById(
                R.id.txtObservacoesItemReserva
            )

        val txtStatus: TextView =
            itemView.findViewById(
                R.id.txtStatusItemReserva
            )

        val btnCancelar: AppCompatButton =
            itemView.findViewById(
                R.id.btnCancelarReserva
            )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ReservaViewHolder {

        val view = LayoutInflater
            .from(parent.context)
            .inflate(
                R.layout.item_minhas_reservas,
                parent,
                false
            )

        return ReservaViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ReservaViewHolder,
        position: Int
    ) {

        val reserva = reservas[position]

        // Data e horário no fuso do restaurante.
        holder.txtDataHora.text = formatarData(
            reserva.inicioUtc
        )

        // Quantidade de pessoas.
        holder.txtQuantidade.text =
            "Quantidade de pessoas: ${reserva.pessoas}"

        // Reutiliza o TextView existente para o código.
        holder.txtObservacoes.text =
            "Código: ${reserva.codigo}"

        // Situação informada pelo servidor.
        val situacao = reserva.situacaoTexto
            .ifBlank { "Não informada" }

        holder.txtStatus.text = buildString {

            append("Situação: $situacao")

            reserva.posicaoFila?.let {
                append("\nPosição na fila: $it")
            }

            reserva.mesa?.takeIf {
                it.isNotBlank()
            }?.let {
                append("\nMesa: $it")
            }
        }

        // A API decide se a reserva pode ser cancelada.
        val permiteCancelar =
            reserva.podeCancelar &&
                    onCancelarClick != null

        holder.btnCancelar.visibility =
            if (permiteCancelar) {
                View.VISIBLE
            } else {
                View.GONE
            }

        holder.btnCancelar.setOnClickListener {
            onCancelarClick?.invoke(reserva)
        }
    }

    /**
     * Converte a data UTC da API para
     * o horário do restaurante em São Paulo.
     */
    private fun formatarData(
        inicioUtc: String?
    ): String {

        if (inicioUtc.isNullOrBlank()) {
            return "Horário a confirmar"
        }

        return try {

            val entrada = SimpleDateFormat(
                "yyyy-MM-dd'T'HH:mm:ss",
                Locale.US
            ).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val data = entrada.parse(
                inicioUtc.take(19)
            ) ?: return "Horário não informado"

            val saida = SimpleDateFormat(
                "dd/MM/yyyy 'às' HH:mm",
                Locale("pt", "BR")
            ).apply {
                timeZone = TimeZone.getTimeZone(
                    "America/Sao_Paulo"
                )
            }

            saida.format(data)

        } catch (_: Exception) {
            "Horário não disponível"
        }
    }

    /**
     * Atualiza a lista com dados reais da API.
     */
    fun atualizarReservas(
        novasReservas: List<ReservaApiResponse>
    ) {

        reservas = novasReservas
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int =
        reservas.size
}