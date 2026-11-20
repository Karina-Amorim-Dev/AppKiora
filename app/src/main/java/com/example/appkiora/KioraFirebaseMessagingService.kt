package com.example.appkiora

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Serviço responsável pelo recebimento de
 * mensagens Firebase Cloud Messaging (FCM).
 */
class KioraFirebaseMessagingService :
    FirebaseMessagingService() {

    companion object {

        private const val CANAL_NOTIFICACOES =
            "kiora_atualizacoes_reservas"

        private const val PREFERENCIAS_NOTIFICACOES =
            "kiora_notificacoes_reservas"

        private const val NOTIFICACOES_ATIVAS =
            "notificacoes_ativas"

        private const val PREFERENCIAS_FCM =
            "kiora_fcm"

        private const val TOKEN_ATUAL =
            "token_atual"
    }

    /**
     * Chamado pelo Firebase quando o token
     * deste dispositivo é criado ou renovado.
     *
     * Por enquanto, o token é salvo localmente.
     * O envio à API será feito na próxima etapa.
     */
    override fun onNewToken(token: String) {

        super.onNewToken(token)

        getSharedPreferences(
            PREFERENCIAS_FCM,
            MODE_PRIVATE
        ).edit()
            .putString(TOKEN_ATUAL, token)
            .apply()
    }

    /**
     * Recebe mensagens entregues ao serviço FCM.
     *
     * Verifica a preferência e a permissão
     * antes de mostrar uma notificação local.
     */
    override fun onMessageReceived(
        mensagem: RemoteMessage
    ) {

        super.onMessageReceived(mensagem)

        val notificacoesAtivas =
            getSharedPreferences(
                PREFERENCIAS_NOTIFICACOES,
                MODE_PRIVATE
            ).getBoolean(
                NOTIFICACOES_ATIVAS,
                false
            )

        if (!notificacoesAtivas) {
            return
        }

        // Permissão exigida no Android 13+.
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val autorizacao =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )

            if (
                autorizacao !=
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

        criarCanalNotificacoes()

        // O conteúdo exibido é genérico,
        // evitando dados pessoais na tela bloqueada.
        val titulo = "Kiora — Reservas"

        val texto =
            "Sua reserva teve uma atualização. " +
                    "Toque para acompanhar."

        val intent = Intent(
            this,
            MinhasReservasActivity::class.java
        ).apply {

            flags =
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val identificador =
            mensagem.data["codigo"]?.hashCode()
                ?: mensagem.messageId?.hashCode()
                ?: System.currentTimeMillis().toInt()

        val pendingIntent =
            PendingIntent.getActivity(
                this,
                identificador,
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
                .setContentTitle(titulo)
                .setContentText(texto)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(
                    NotificationCompat.PRIORITY_DEFAULT
                )
                .build()

        try {

            gerenciador.notify(
                identificador,
                notificacao
            )

        } catch (_: SecurityException) {

            // A permissão pode ser revogada
            // entre a verificação e o envio.
            getSharedPreferences(
                PREFERENCIAS_NOTIFICACOES,
                MODE_PRIVATE
            ).edit()
                .putBoolean(
                    NOTIFICACOES_ATIVAS,
                    false
                )
                .apply()
        }
    }

    /**
     * Utiliza o mesmo canal de notificações
     * criado na tela Minhas Reservas.
     */
    private fun criarCanalNotificacoes() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val canal = NotificationChannel(
                CANAL_NOTIFICACOES,
                "Atualizações de reservas Kiora",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {

                description =
                    "Notificações sobre alterações nas reservas."
            }

            getSystemService(
                NotificationManager::class.java
            ).createNotificationChannel(canal)
        }
    }
}