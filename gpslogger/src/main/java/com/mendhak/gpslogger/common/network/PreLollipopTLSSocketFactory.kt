package com.mendhak.gpslogger.common.network

import android.net.SSLCertificateSocketFactory
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import android.R.attr.host
import android.os.Build
import android.os.Build.VERSION_CODES.JELLY_BEAN_MR1
import android.os.Build.VERSION.SDK_INT
import android.util.Log


/*
  Android API levels 16 to 21 have TLS *supported* but not *enabled*
  This asinine class exists purely to override the default socket creation
  and enable all existing supported protocols.
 */

//class PreLollipopTLSSocketFactory @Throws(KeyManagementException::class, NoSuchAlgorithmException::class)
class PreLollipopTLSSocketFactory
    constructor(trustManagers: Array<TrustManager>) : SSLSocketFactory() {

//    private val delegate: SSLSocketFactory
    private val delegate: SSLCertificateSocketFactory

    init {
//        val context = SSLContext.getInstance("TLS")
//        context.init(null, trustManagers, null)
//        delegate = context.socketFactory

        delegate = SSLCertificateSocketFactory.getDefault(0) as SSLCertificateSocketFactory
        delegate.setTrustManagers(trustManagers)
    }

    override fun getDefaultCipherSuites(): Array<String> {
        return delegate.defaultCipherSuites
    }

    override fun getSupportedCipherSuites(): Array<String> {
        return delegate.supportedCipherSuites
    }

    @Throws(IOException::class)
    override fun createSocket(): Socket {

        return enableTLSOnSocket(delegate.createSocket())
    }

    @Throws(IOException::class)
    override fun createSocket(s: Socket, host: String, port: Int, autoClose: Boolean): Socket {
        var sock = delegate.createSocket(s, host, port,autoClose)
        sock = enableSNIOnSocket(sock,host)
        return enableTLSOnSocket(sock)
    }


    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int): Socket {
        var sock = delegate.createSocket(InetAddress.getByName(host), port)
        sock = enableSNIOnSocket(sock, host)
        return enableTLSOnSocket(sock)
    }

    @Throws(IOException::class, UnknownHostException::class)
    override fun createSocket(host: String, port: Int, localHost: InetAddress, localPort: Int): Socket {
        var sock = delegate.createSocket(host, port, localHost, localPort)
        sock = enableSNIOnSocket(sock, host)
        return enableTLSOnSocket(sock)
    }

    @Throws(IOException::class)
    override fun createSocket(host: InetAddress, port: Int): Socket {
        var sock = delegate.createSocket(host, port)
        sock = enableSNIOnSocket(sock, host.hostName)
        return enableTLSOnSocket(sock)
    }

    @Throws(IOException::class)
    override fun createSocket(address: InetAddress, port: Int, localAddress: InetAddress, localPort: Int): Socket {
        var sock = delegate.createSocket(address, port, localAddress, localPort)
        sock = enableSNIOnSocket(sock, address.hostName)
        return enableTLSOnSocket(sock)
    }

    private fun enableTLSOnSocket(socket: Socket): Socket {
        if (socket != null && socket is SSLSocket) {
            socket.enabledProtocols = socket.supportedProtocols
        }
        return socket
    }

    private fun enableSNIOnSocket(sock: Socket, host: String): Socket {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            delegate.setHostname(sock, host)
        } else {
            try {
                val setHostnameMethod = sock::class.java.getMethod("setHostname", String::class.java)
                setHostnameMethod.invoke(sock, host)
            } catch (e: Exception) {
                Log.d(PreLollipopTLSSocketFactory::class.java!!.getSimpleName(), "SNI not usable: $e")
            }

        }

        return sock

    }

}