package com.helpdesk.app.data.api

import android.util.Log
import com.helpdesk.app.BuildConfig
import com.helpdesk.app.data.repository.AuthRepository
import com.helpdesk.app.domain.model.Ticket
import com.helpdesk.app.domain.model.TicketActivityEntry
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

sealed class SocketEvent {
    data class TicketCreated(val ticket: Ticket) : SocketEvent()
    data class TicketUpdated(val ticket: Ticket) : SocketEvent()
    data class TicketActivity(val ticketId: String, val activity: TicketActivityEntry) : SocketEvent()
    object Connected : SocketEvent()
    object Disconnected : SocketEvent()
    data class Error(val message: String) : SocketEvent()
}

@Singleton
class SocketManager @Inject constructor(
    private val authRepository: AuthRepository
) {
    private var socket: Socket? = null
    private var currentToken: String? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<SocketEvent>(replay = 0, extraBufferCapacity = 64)
    val events: SharedFlow<SocketEvent> = _events.asSharedFlow()

    private val connectionListener = Emitter.Listener {
        Log.i(TAG, "Socket connected")
        scope.launch {
            _events.emit(SocketEvent.Connected)
        }
    }

    private val disconnectListener = Emitter.Listener { args ->
        val reason = args.getOrNull(0)?.toString() ?: "Unknown"
        Log.i(TAG, "Socket disconnected: $reason")
        scope.launch {
            _events.emit(SocketEvent.Disconnected)
        }
    }

    private val connectErrorListener = Emitter.Listener { args ->
        val error = args.getOrNull(0)?.toString() ?: "Unknown error"
        Log.w(TAG, "Socket connection error: $error")
        scope.launch {
            _events.emit(SocketEvent.Error(error))
        }

        // If auth-related error, try to refresh session
        if (error.contains("invalid", ignoreCase = true) ||
            error.contains("expired", ignoreCase = true) ||
            error.contains("token", ignoreCase = true)
        ) {
            scope.launch {
                authRepository.refreshSession()
                reconnect()
            }
        }
    }

    private val ticketsCreatedListener = Emitter.Listener { args ->
        try {
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val ticketJson = data.getJSONObject("ticket")
            val ticket = parseTicketFromJson(ticketJson)
            scope.launch {
                _events.emit(SocketEvent.TicketCreated(ticket))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tickets:created event", e)
        }
    }

    private val ticketsUpdatedListener = Emitter.Listener { args ->
        try {
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val ticketJson = data.getJSONObject("ticket")
            val ticket = parseTicketFromJson(ticketJson)
            scope.launch {
                _events.emit(SocketEvent.TicketUpdated(ticket))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tickets:updated event", e)
        }
    }

    private val ticketsActivityListener = Emitter.Listener { args ->
        try {
            val data = args.getOrNull(0) as? JSONObject ?: return@Listener
            val ticketId = data.getString("ticketId")
            val activityJson = data.getJSONObject("activity")
            val activity = parseActivityFromJson(activityJson)
            scope.launch {
                _events.emit(SocketEvent.TicketActivity(ticketId, activity))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing tickets:activity event", e)
        }
    }

    fun syncSession(accessToken: String?) {
        if (accessToken == null) {
            disconnect()
            return
        }

        if (socket != null && currentToken == accessToken) {
            if (!socket!!.connected()) {
                socket!!.connect()
            }
            return
        }

        disconnect()

        try {
            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                auth = mapOf("token" to "Bearer $accessToken")
                reconnection = true
                reconnectionAttempts = 5
                reconnectionDelay = 1000
                timeout = 20000
            }

            socket = IO.socket(BuildConfig.API_BASE_URL, options).apply {
                on(Socket.EVENT_CONNECT, connectionListener)
                on(Socket.EVENT_DISCONNECT, disconnectListener)
                on(Socket.EVENT_CONNECT_ERROR, connectErrorListener)
                on("tickets:created", ticketsCreatedListener)
                on("tickets:updated", ticketsUpdatedListener)
                on("tickets:activity", ticketsActivityListener)
                connect()
            }

            currentToken = accessToken
            Log.i(TAG, "Socket connecting to ${BuildConfig.API_BASE_URL}")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating socket connection", e)
            scope.launch {
                _events.emit(SocketEvent.Error(e.message ?: "Socket connection failed"))
            }
        }
    }

    fun disconnect() {
        socket?.let {
            it.off()
            if (it.connected()) {
                it.disconnect()
            }
        }
        socket = null
        currentToken = null
    }

    fun reconnect() {
        val token = currentToken
        disconnect()
        if (token != null) {
            syncSession(token)
        }
    }

    private fun parseTicketFromJson(json: JSONObject): Ticket {
        val creatorJson = json.getJSONObject("creator")
        val assigneeJson = json.optJSONObject("assignee")
        val assignmentRequestJson = json.optJSONObject("assignmentRequest")
        val attachmentsArray = json.optJSONArray("attachments")

        val attachments = mutableListOf<String>()
        if (attachmentsArray != null) {
            for (i in 0 until attachmentsArray.length()) {
                attachments.add(attachmentsArray.getString(i))
            }
        }

        return Ticket(
            id = json.getString("id"),
            description = json.getString("description"),
            priority = com.helpdesk.app.domain.model.TicketPriority.fromString(json.getString("priority")),
            issueType = com.helpdesk.app.domain.model.IssueType.fromString(json.getString("issueType")),
            status = com.helpdesk.app.domain.model.TicketStatus.fromString(json.getString("status")),
            attachments = attachments,
            createdAt = json.getString("createdAt"),
            updatedAt = json.getString("updatedAt"),
            resolvedAt = json.optString("resolvedAt", null),
            creator = parseTicketUserFromJson(creatorJson),
            assignee = assigneeJson?.let { parseTicketUserFromJson(it) },
            assignmentRequest = assignmentRequestJson?.let { parseTicketUserFromJson(it) }
        )
    }

    private fun parseTicketUserFromJson(json: JSONObject): com.helpdesk.app.domain.model.TicketUser {
        return com.helpdesk.app.domain.model.TicketUser(
            id = json.getString("id"),
            name = json.getString("name"),
            email = json.getString("email")
        )
    }

    private fun parseActivityFromJson(json: JSONObject): TicketActivityEntry {
        val actorJson = json.getJSONObject("actor")
        val fromAssigneeJson = json.optJSONObject("fromAssignee")
        val toAssigneeJson = json.optJSONObject("toAssignee")

        return TicketActivityEntry(
            id = json.getString("id"),
            ticketId = json.getString("ticketId"),
            type = com.helpdesk.app.domain.model.TicketActivityType.fromString(json.getString("type")),
            createdAt = json.getString("createdAt"),
            actor = com.helpdesk.app.domain.model.TicketActivityActor(
                id = actorJson.getString("id"),
                name = actorJson.getString("name"),
                email = actorJson.getString("email"),
                role = com.helpdesk.app.domain.model.UserRole.fromString(actorJson.getString("role"))
            ),
            fromStatus = json.optString("fromStatus", null)?.let {
                com.helpdesk.app.domain.model.TicketStatus.fromString(it)
            },
            toStatus = json.optString("toStatus", null)?.let {
                com.helpdesk.app.domain.model.TicketStatus.fromString(it)
            },
            fromAssignee = fromAssigneeJson?.let { parseTicketUserFromJson(it) },
            toAssignee = toAssigneeJson?.let { parseTicketUserFromJson(it) }
        )
    }

    companion object {
        private const val TAG = "SocketManager"
    }
}
