package com.ioristudios.filemanager.sharing.transport

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

/**
 * Robust TCP transport layer for peer-to-peer file transfer.
 * Handles framing, length-prefixed packet streaming, and socket timeouts.
 */
class SocketTransport(private val socket: Socket) {

    private val inputStream = DataInputStream(socket.getInputStream())
    private val outputStream = DataOutputStream(socket.getOutputStream())

    val isConnected: Boolean
        get() = socket.isConnected && !socket.isClosed

    /**
     * Sends a 4-byte big-endian length-prefixed packet.
     */
    suspend fun sendFrame(bytes: ByteArray) = withContext(Dispatchers.IO) {
        synchronized(outputStream) {
            outputStream.writeInt(bytes.size)
            outputStream.write(bytes)
            outputStream.flush()
        }
    }

    /**
     * Reads a 4-byte big-endian length-prefixed packet.
     */
    suspend fun receiveFrame(): ByteArray = withContext(Dispatchers.IO) {
        synchronized(inputStream) {
            val length = inputStream.readInt()
            if (length < 0 || length > 16 * 1024 * 1024) { // 16MB frame limit
                throw IllegalStateException("Invalid packet length: $length")
            }
            val buffer = ByteArray(length)
            inputStream.readFully(buffer)
            buffer
        }
    }

    fun close() {
        try {
            socket.close()
        } catch (_: Exception) {}
    }

    companion object {
        private const val TAG = "SocketTransport"

        /**
         * Connects to a remote host and port with automatic retry and proper option timing.
         * Crucial fix: Socket options (like tcpNoDelay) must only be set AFTER native connect,
         * avoiding Android bionic libc ECONNREFUSED/EINVAL on unconnected descriptors.
         */
        suspend fun connect(
            host: String,
            port: Int,
            timeoutMs: Int = 10000,
            maxRetries: Int = 2
        ): SocketTransport = withContext(Dispatchers.IO) {
            var lastException: Exception? = null

            for (attempt in 1..maxRetries) {
                var socket: Socket? = null
                try {
                    Log.d(TAG, "Connecting to $host:$port (attempt $attempt/$maxRetries)...")
                    val targetAddress = InetAddress.getByName(host)
                    socket = Socket()
                    socket.reuseAddress = true
                    // Connect first to establish native descriptor
                    socket.connect(InetSocketAddress(targetAddress, port), timeoutMs)

                    // Configure socket parameters only after connection is active
                    try {
                        socket.tcpNoDelay = true
                        socket.soTimeout = 45000 // 45s read timeout
                        socket.sendBufferSize = 128 * 1024
                        socket.receiveBufferSize = 128 * 1024
                    } catch (e: Exception) {
                        Log.w(TAG, "Warning configuring connected socket options: ${e.message}")
                    }

                    Log.d(TAG, "Successfully connected to $host:$port")
                    return@withContext SocketTransport(socket)
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "Connection attempt $attempt to $host:$port failed: ${e.message}")
                    try {
                        socket?.close()
                    } catch (_: Exception) {}

                    if (attempt < maxRetries) {
                        delay(600) // Brief delay before retrying
                    }
                }
            }

            throw lastException ?: java.net.ConnectException("Failed to connect to $host:$port after $maxRetries attempts")
        }

        suspend fun createServer(port: Int = 0): Pair<ServerSocket, Int> =
            withContext(Dispatchers.IO) {
                val serverSocket = ServerSocket()
                serverSocket.reuseAddress = true
                serverSocket.bind(InetSocketAddress(port))
                Pair(serverSocket, serverSocket.localPort)
            }
    }
}
