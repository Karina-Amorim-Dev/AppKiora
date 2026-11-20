
package com.example.appkiora

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class CardapioActivity : AppCompatActivity() {

    private val cardapioRepository = CardapioRepository()

    // Controla as consultas de produtos.
    private var carregamentoProdutosJob: Job? = null

    // Controla a consulta dos detalhes de um produto.
    private var detalheProdutoJob: Job? = null

    private lateinit var containerColecoes: LinearLayout
    private lateinit var containerCategorias: LinearLayout
    private lateinit var containerProdutos: LinearLayout

    private lateinit var txtTituloResultados: TextView
    private lateinit var txtPeriodoMaisPedidos: TextView
    private lateinit var txtMensagem: TextView

    private lateinit var edtBusca: EditText
    private lateinit var edtPrecoMinimo: EditText
    private lateinit var edtPrecoMaximo: EditText

    private lateinit var spnOrdenacao: Spinner
    private lateinit var chkSomenteDisponiveis: CheckBox
    private lateinit var chkSomentePromocoes: CheckBox

    private var categoriaSelecionadaId: Int? = null
    private var colecaoSelecionada: String? = null

    private val formatoMoeda = NumberFormat.getCurrencyInstance(
        Locale("pt", "BR")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_cardapio)

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

        inicializarComponentes()
        configurarOrdenacao()
        configurarFiltros()
        carregarCategorias()
    }

    // Localiza os componentes do layout.
    private fun inicializarComponentes() {

        containerColecoes =
            findViewById(R.id.containerColecoesCardapio)

        containerCategorias =
            findViewById(R.id.containerCategoriasCardapio)

        containerProdutos =
            findViewById(R.id.containerProdutosCardapio)

        txtTituloResultados =
            findViewById(R.id.txtTituloResultadosCardapio)

        txtPeriodoMaisPedidos =
            findViewById(R.id.txtPeriodoMaisPedidos)

        txtMensagem =
            findViewById(R.id.txtMensagemCardapio)

        edtBusca =
            findViewById(R.id.edtBuscaCardapio)

        edtPrecoMinimo =
            findViewById(R.id.edtPrecoMinimo)

        edtPrecoMaximo =
            findViewById(R.id.edtPrecoMaximo)

        spnOrdenacao =
            findViewById(R.id.spnOrdenacaoCardapio)

        chkSomenteDisponiveis =
            findViewById(R.id.chkSomenteDisponiveis)

        chkSomentePromocoes =
            findViewById(R.id.chkSomentePromocoes)
    }

    // Configura as opções de ordenação.
    private fun configurarOrdenacao() {

        val opcoes = listOf(
            "Padrão",
            "Menor preço",
            "Maior preço",
            "Nome: A-Z",
            "Nome: Z-A"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcoes
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spnOrdenacao.adapter = adapter
    }

    // Configura o botão de aplicação dos filtros.
    private fun configurarFiltros() {

        findViewById<AppCompatButton>(R.id.btnAplicarFiltros)
            .setOnClickListener {
                carregarProdutos()
            }
    }

    // Consulta as categorias e prepara os botões.
    private fun carregarCategorias() {

        lifecycleScope.launch {

            try {

                val categorias =
                    cardapioRepository.listarCategorias()

                criarBotoesColecoes()
                criarBotoesCategorias(categorias)

                carregarProdutos()

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                exibirMensagem(
                    erro.message
                        ?: "Não foi possível carregar as categorias. Verifique sua conexão e tente novamente."
                )
            }
        }
    }

    // Cria os botões das coleções disponíveis.
    private fun criarBotoesColecoes() {

        containerColecoes.removeAllViews()

        adicionarBotao(
            containerColecoes,
            "Todos os produtos"
        ) {
            categoriaSelecionadaId = null
            colecaoSelecionada = null
            carregarProdutos()
        }

        adicionarBotao(
            containerColecoes,
            "Mais pedidos"
        ) {
            categoriaSelecionadaId = null
            colecaoSelecionada = "mais-pedidos"
            carregarProdutos()
        }

        adicionarBotao(
            containerColecoes,
            "Combos"
        ) {
            categoriaSelecionadaId = null
            colecaoSelecionada = "combos"
            carregarProdutos()
        }

        adicionarBotao(
            containerColecoes,
            "Promoções"
        ) {
            categoriaSelecionadaId = null
            colecaoSelecionada = "promocoes"
            carregarProdutos()
        }
    }

    // Cria os botões conforme as categorias retornadas pela API.
    private fun criarBotoesCategorias(
        categorias: List<Categoria>
    ) {

        containerCategorias.removeAllViews()

        categorias.forEach { categoria ->

            adicionarBotao(
                containerCategorias,
                categoria.nome
            ) {
                colecaoSelecionada = null
                categoriaSelecionadaId = categoria.id
                carregarProdutos()
            }
        }
    }

    // Cria um botão utilizando o padrão visual do Kiora.
    private fun adicionarBotao(
        container: LinearLayout,
        texto: String,
        acao: () -> Unit
    ) {

        val botao = AppCompatButton(this).apply {

            text = texto.uppercase(Locale("pt", "BR"))
            textSize = 14f

            setTextColor(android.graphics.Color.WHITE)
            setBackgroundResource(R.drawable.bg_botao_kiora)

            backgroundTintList = null
            isAllCaps = false

            setOnClickListener {
                acao()
            }
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            55.dp()
        ).apply {

            bottomMargin = 12.dp()
            marginStart = 16.dp()
            marginEnd = 16.dp()
        }

        container.addView(botao, parametros)
    }

    // Consulta os produtos aplicando os filtros selecionados.
    private fun carregarProdutos() {

        carregamentoProdutosJob?.cancel()

        containerProdutos.removeAllViews()
        containerProdutos.visibility = View.VISIBLE

        txtMensagem.visibility = View.GONE
        txtPeriodoMaisPedidos.visibility = View.GONE

        val busca = edtBusca.text.toString()
            .trim()
            .ifEmpty { null }

        val precoMinimo = edtPrecoMinimo.text.toString()
            .trim()
            .ifEmpty { null }

        val precoMaximo = edtPrecoMaximo.text.toString()
            .trim()
            .ifEmpty { null }

        val ordenacao = when (spnOrdenacao.selectedItemPosition) {
            1 -> "menor-preco"
            2 -> "maior-preco"
            3 -> "nome-az"
            4 -> "nome-za"
            else -> "padrao"
        }

        val categoriaId = categoriaSelecionadaId
        val colecao = colecaoSelecionada

        val somenteDisponiveis =
            chkSomenteDisponiveis.isChecked

        val somentePromocoes =
            chkSomentePromocoes.isChecked

        carregamentoProdutosJob = lifecycleScope.launch {

            try {

                val resposta = cardapioRepository.listarProdutos(
                    categoriaId = categoriaId,
                    colecao = colecao,
                    busca = busca,
                    precoMinimo = precoMinimo,
                    precoMaximo = precoMaximo,
                    ordenacao = ordenacao,
                    somenteDisponiveis = somenteDisponiveis,
                    somentePromocoes = somentePromocoes
                )

                txtTituloResultados.text = resposta.titulo

                if (!resposta.periodoMaisPedidos.isNullOrBlank()) {

                    txtPeriodoMaisPedidos.text =
                        resposta.periodoMaisPedidos

                    txtPeriodoMaisPedidos.visibility =
                        View.VISIBLE
                }

                if (resposta.errosFiltros.isNotEmpty()) {

                    exibirMensagem(
                        resposta.errosFiltros.joinToString("\n")
                    )

                    return@launch
                }

                if (resposta.produtos.isEmpty()) {

                    exibirMensagem(
                        "Nenhum produto encontrado com os filtros selecionados."
                    )

                    return@launch
                }

                exibirProdutos(resposta.produtos)

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                exibirMensagem(
                    erro.message
                        ?: "Não foi possível carregar os produtos. Verifique sua conexão e tente novamente."
                )
            }
        }
    }

    // Exibe os produtos retornados pela API.
    private fun exibirProdutos(produtos: List<Produto>) {

        containerProdutos.removeAllViews()

        produtos.forEach { produto ->

            val itemProduto = layoutInflater.inflate(
                R.layout.item_produto_cardapio,
                containerProdutos,
                false
            )

            val imgProduto = itemProduto.findViewById<ImageView>(
                R.id.imgProduto
            )

            val txtCombo = itemProduto.findViewById<TextView>(
                R.id.txtComboProduto
            )

            val txtNome = itemProduto.findViewById<TextView>(
                R.id.txtNomeProduto
            )

            val txtDescricao = itemProduto.findViewById<TextView>(
                R.id.txtDescricaoProduto
            )

            val txtPrecoOriginal = itemProduto.findViewById<TextView>(
                R.id.txtPrecoOriginalProduto
            )

            val txtDesconto = itemProduto.findViewById<TextView>(
                R.id.txtDescontoProduto
            )

            val txtPreco = itemProduto.findViewById<TextView>(
                R.id.txtPrecoProduto
            )

            val txtDisponibilidade =
                itemProduto.findViewById<TextView>(
                    R.id.txtDisponibilidadeProduto
                )

            // Apresenta as informações principais.
            txtNome.text = produto.nome
            txtDescricao.text = produto.descricao
            txtPreco.text = formatoMoeda.format(produto.preco)

            // Carrega a imagem recebida da API.
            val imagem = produto.imagemUrl

            if (!imagem.isNullOrBlank()) {

                imgProduto.visibility = View.VISIBLE
                imgProduto.load(montarUrlImagem(imagem))

            } else {
                imgProduto.visibility = View.GONE
            }

            // Identifica os combos.
            if (produto.ehCombo) {
                txtCombo.visibility = View.VISIBLE
            } else {
                txtCombo.visibility = View.GONE
            }

            // Apresenta o preço original e o desconto.
            if (produto.descontoPercentual > 0) {

                txtPrecoOriginal.text =
                    "De: ${formatoMoeda.format(produto.precoOriginal)}"

                txtPrecoOriginal.visibility = View.VISIBLE

                txtDesconto.text =
                    "${produto.descontoPercentual}% de desconto"

                txtDesconto.visibility = View.VISIBLE

            } else {

                txtPrecoOriginal.visibility = View.GONE
                txtDesconto.visibility = View.GONE
            }

            // Apresenta a disponibilidade do produto.
            txtDisponibilidade.text = if (produto.disponivel) {
                "Disponível"
            } else {
                "Indisponível"
            }

            // Abre os detalhes ao tocar no cartão.
            itemProduto.findViewById<LinearLayout>(
                R.id.cardProduto
            ).setOnClickListener {
                abrirDetalhesProduto(produto.id)
            }

            containerProdutos.addView(itemProduto)
        }
    }


    // Consulta os detalhes atualizados de um produto pela API.
    private fun abrirDetalhesProduto(id: Int) {

        detalheProdutoJob?.cancel()

        // Cria o conteúdo visual do carregamento.
        val densidade = resources.displayMetrics.density
        fun dp(valor: Int): Int = (valor * densidade).toInt()

        val conteudoCarregamento = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(20), dp(24), dp(20))
            setBackgroundResource(R.drawable.bg_dialog_minha_conta)
        }

        val indicador = android.widget.ProgressBar(this)

        val mensagemCarregamento = TextView(this).apply {
            text = "Carregando detalhes do produto..."
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setPadding(dp(16), 0, 0, 0)
        }

        conteudoCarregamento.addView(indicador)
        conteudoCarregamento.addView(
            mensagemCarregamento,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        // Exibe o carregamento personalizado.
        val dialogCarregamento = AlertDialog.Builder(this)
            .setView(conteudoCarregamento)
            .setCancelable(false)
            .create()

        dialogCarregamento.show()
        dialogCarregamento.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        detalheProdutoJob = lifecycleScope.launch {
            try {

                val produto = cardapioRepository.buscarProduto(id)

                if (!isFinishing && !isDestroyed) {
                    exibirDetalhesProduto(produto)
                }

            } catch (erro: CancellationException) {
                throw erro

            } catch (erro: Exception) {

                Toast.makeText(
                    this@CardapioActivity,
                    erro.message
                        ?: "Não foi possível carregar os detalhes do produto.",
                    Toast.LENGTH_LONG
                ).show()

            } finally {
                // Fecha o carregamento quando a consulta terminar.
                if (dialogCarregamento.isShowing) {
                    dialogCarregamento.dismiss()
                }
            }
        }
    }


    // Monta e apresenta o modal personalizado do Kiora.
    private fun exibirDetalhesProduto(produto: Produto) {

        val conteudo = layoutInflater.inflate(
            R.layout.dialog_detalhes_produto,
            null
        )

        val imgDetalhes = conteudo.findViewById<ImageView>(
            R.id.imgDetalhesProduto
        )

        val txtCombo = conteudo.findViewById<TextView>(
            R.id.txtComboDetalhesProduto
        )

        val txtNome = conteudo.findViewById<TextView>(
            R.id.txtNomeDetalhesProduto
        )

        val txtCategoria = conteudo.findViewById<TextView>(
            R.id.txtCategoriaDetalhesProduto
        )

        val txtDescricao = conteudo.findViewById<TextView>(
            R.id.txtDescricaoDetalhesProduto
        )

        val txtPrecoOriginal = conteudo.findViewById<TextView>(
            R.id.txtPrecoOriginalDetalhesProduto
        )

        val txtDesconto = conteudo.findViewById<TextView>(
            R.id.txtDescontoDetalhesProduto
        )

        val txtPreco = conteudo.findViewById<TextView>(
            R.id.txtPrecoDetalhesProduto
        )

        val txtDisponibilidade =
            conteudo.findViewById<TextView>(
                R.id.txtDisponibilidadeDetalhesProduto
            )

        // Preenche as informações recebidas da API.
        txtNome.text = produto.nome
        txtCategoria.text = produto.nomeCategoria
        txtDescricao.text = produto.descricao
        txtPreco.text = formatoMoeda.format(produto.preco)

        // Carrega a imagem ampliada.
        val imagem = produto.imagemUrl

        if (!imagem.isNullOrBlank()) {

            imgDetalhes.visibility = View.VISIBLE
            imgDetalhes.load(montarUrlImagem(imagem))

        } else {
            imgDetalhes.visibility = View.GONE
        }

        // Identifica os combos.
        if (produto.ehCombo) {
            txtCombo.visibility = View.VISIBLE
        } else {
            txtCombo.visibility = View.GONE
        }

        // Exibe os valores promocionais, quando existentes.
        if (produto.descontoPercentual > 0) {

            txtPrecoOriginal.text =
                "De: ${formatoMoeda.format(produto.precoOriginal)}"

            txtPrecoOriginal.visibility = View.VISIBLE

            txtDesconto.text =
                "${produto.descontoPercentual}% de desconto"

            txtDesconto.visibility = View.VISIBLE

        } else {

            txtPrecoOriginal.visibility = View.GONE
            txtDesconto.visibility = View.GONE
        }

        // Apresenta a disponibilidade atual.
        txtDisponibilidade.text = if (produto.disponivel) {
            "Disponível"
        } else {
            "Indisponível"
        }

        // Cria o diálogo com o layout personalizado.
        val dialog = AlertDialog.Builder(this)
            .setView(conteudo)
            .create()

        dialog.setCancelable(true)
        dialog.show()

        // Remove o fundo padrão do diálogo.
        dialog.window?.setBackgroundDrawableResource(
            android.R.color.transparent
        )

        // Ajusta a largura para respeitar as margens da tela.
        dialog.window?.setLayout(
            resources.displayMetrics.widthPixels - 32.dp(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Configura o botão de fechamento.
        conteudo.findViewById<AppCompatButton>(
            R.id.btnFecharDetalhesProduto
        ).setOnClickListener {
            dialog.dismiss()
        }
    }

    // Monta a URL completa da imagem.
    private fun montarUrlImagem(imagem: String): String {

        return if (imagem.startsWith("http", ignoreCase = true)) {
            imagem
        } else {
            ApiConfig.BASE_URL.trimEnd('/') +
                    "/" + imagem.trimStart('/')
        }
    }

    // Apresenta mensagens de erro ou ausência de resultados.
    private fun exibirMensagem(texto: String) {

        containerProdutos.removeAllViews()
        containerProdutos.visibility = View.GONE

        txtMensagem.text = texto
        txtMensagem.visibility = View.VISIBLE
    }

    // Converte valores de dp para pixels.
    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {

        carregamentoProdutosJob?.cancel()
        detalheProdutoJob?.cancel()

        super.onDestroy()
    }
}
