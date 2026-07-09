package com.uicontrol.ui24remote

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var setupContainer: android.widget.LinearLayout
    private lateinit var webViewContainer: android.widget.FrameLayout
    private lateinit var webView: WebView
    private lateinit var loadingSpinner: android.widget.ProgressBar

    companion object {
        private const val PREFS_NAME = "ui24_remote_prefs"
        private const val KEY_MODE = "mode"          // "ip" or "demo"
        private const val KEY_IP = "saved_ip"
        private const val DEMO_URL = "https://www.soundcraft.com/ui24-software-demo/mixer.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mantém a tela sempre ligada, como um app de jogo/console
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Pede a MAIOR taxa de atualização de tela disponível (90Hz/120Hz em
        // vez de travar em 60Hz). Navegadores pedem isso automaticamente;
        // apps customizados não pedem por padrão, e isso sozinho já parece
        // "travado" ao arrastar fader/slider mesmo sem travar de verdade.
        requestHighestRefreshRate()

        // Pede ao sistema pra manter desempenho consistente durante uso
        // prolongado e intenso (em vez de deixar o Android reduzir o
        // desempenho depois de uns minutos de app aberto).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            window.setSustainedPerformanceMode(true)
        }

        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupContainer = findViewById(R.id.setupContainer)
        webViewContainer = findViewById(R.id.webViewContainer)
        webView = findViewById(R.id.webView)
        loadingSpinner = findViewById(R.id.loadingSpinner)

        setupWebView()
        setupDisconnectButton()
        setupConfigScreenButtons()

        applyImmersiveFullscreen()

        // Se já existe uma mesa configurada, entra direto nela.
        val savedMode = prefs.getString(KEY_MODE, null)
        if (savedMode != null) {
            openTable(savedMode, prefs.getString(KEY_IP, ""))
        } else {
            showSetupScreen()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveFullscreen()
    }

    /**
     * Força a janela a usar a maior taxa de atualização de tela suportada
     * pelo tablet (ex: 120Hz em vez do padrão de 60Hz). Isso é o que faz
     * arrastar fader/slider parecer "liso" igual no navegador.
     */
    private fun requestHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val bestMode = display.supportedModes.maxByOrNull { it.refreshRate }
            if (bestMode != null) {
                val params = window.attributes
                params.preferredDisplayModeId = bestMode.modeId
                window.attributes = params
            }
        }
    }

    /**
     * Ativa o modo tela cheia imersivo (o mesmo usado por jogos), escondendo
     * barra de status e barra de navegação. As barras só reaparecem se o
     * usuário arrastar da borda, e depois somem de novo automaticamente.
     */
    private fun applyImmersiveFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            // "overview mode" / "wide viewport" são para sites antigos, não
            // feitos pra celular. A UI24 já é responsiva; deixar isso ligado
            // força recálculos de zoom/layout desnecessários e pesa o app.
            loadWithOverviewMode = false
            useWideViewPort = false
            mediaPlaybackRequiresUserGesture = false
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            // offscreenPreRaster desligado: é útil pra páginas com scroll longo,
            // mas a UI24 é um dashboard com VU-meters animando o tempo todo.
            // Com isso ligado, a WebView fica rasterizando canais fora da tela
            // sem necessidade, gastando GPU à toa — provável causa de travamento.
            offscreenPreRaster = false
        }

        // Reforça renderização via GPU na própria WebView (redundante com
        // android:hardwareAccelerated no manifest, mas deixa explícito e
        // fácil de testar trocando para LAYER_TYPE_NONE se precisar comparar).
        webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

        // Tira o efeito de "elástico" nas bordas (menos redraw ao arrastar faders perto da borda).
        webView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
        // Some com a barra de rolagem — some visual, um a menos pra desenhar a cada frame.
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                loadingSpinner.visibility = android.view.View.VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingSpinner.visibility = android.view.View.GONE
            }
        }
    }

    private fun setupConfigScreenButtons() {
        val ipInput: TextInputEditText = findViewById(R.id.ipInput)
        val connectButton: MaterialButton = findViewById(R.id.connectButton)
        val demoButton: MaterialButton = findViewById(R.id.demoButton)

        connectButton.setOnClickListener {
            val typedIp = ipInput.text?.toString()?.trim().orEmpty()
            if (typedIp.isEmpty()) {
                ipInput.error = "Digite o IP da mesa"
                return@setOnClickListener
            }
            prefs.edit()
                .putString(KEY_MODE, "ip")
                .putString(KEY_IP, typedIp)
                .apply()
            openTable("ip", typedIp)
        }

        demoButton.setOnClickListener {
            prefs.edit()
                .putString(KEY_MODE, "demo")
                .putString(KEY_IP, "")
                .apply()
            openTable("demo", "")
        }
    }

    /**
     * Pequeno botão no canto superior esquerdo. Um toque pede confirmação
     * para desconectar da mesa atual e liberar a troca do IP.
     */
    private fun setupDisconnectButton() {
        val disconnectButton = findViewById<android.view.View>(R.id.disconnectButton)
        disconnectButton.setOnClickListener { confirmDisconnect() }
    }

    private fun confirmDisconnect() {
        AlertDialog.Builder(this)
            .setTitle("Desconectar da mesa?")
            .setMessage("Isso vai apagar o IP salvo e voltar para a tela de configuração.")
            .setPositiveButton("Desconectar") { _, _ ->
                prefs.edit().clear().apply()
                webView.loadUrl("about:blank")
                showSetupScreen()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showSetupScreen() {
        setupContainer.visibility = android.view.View.VISIBLE
        webViewContainer.visibility = android.view.View.GONE
    }

    private fun openTable(mode: String, ip: String?) {
        setupContainer.visibility = android.view.View.GONE
        webViewContainer.visibility = android.view.View.VISIBLE

        val url = if (mode == "demo") {
            DEMO_URL
        } else {
            val cleanIp = (ip ?: "").trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .trimEnd('/')
            "http://$cleanIp/mixer.html"
        }
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        // Modo "kiosk": o botão voltar não sai do app nem sai da mesa.
        // Dentro da própria página da mesa, deixa navegar entre as telas dela.
        if (webViewContainer.visibility == android.view.View.VISIBLE && webView.canGoBack()) {
            webView.goBack()
        }
        // Caso contrário, ignora o back (não fecha o app sozinho).
    }
}
