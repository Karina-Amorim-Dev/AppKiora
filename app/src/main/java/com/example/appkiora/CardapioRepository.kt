package com.example.appkiora

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import java.text.Normalizer
import java.util.Locale

/**
 * Intermedia a comunicação entre o cardápio
 * do aplicativo e a API do Kiora.
 */
class CardapioRepository {

    private val api = RetrofitClient.instancia
        .create(CardapioApiService::class.java)

    private var categoriasCarregadas: List<Categoria> = emptyList()

    // Busca as categorias diretamente no servidor.
    suspend fun listarCategorias(): List<Categoria> =
        withContext(Dispatchers.IO) {

            val resposta = api.listarCategorias()

            if (resposta.isSuccessful && resposta.body() != null) {
                val categorias = resposta.body()!!
                categoriasCarregadas = categorias
                categorias
            } else {
                throw Exception(
                    "Não foi possível carregar as categorias. " +
                            "Código: ${resposta.code()}"
                )
            }
        }

    // Consulta os produtos e aplica os filtros no Android.
    suspend fun listarProdutos(
        categoriaId: Int? = null,
        colecao: String? = null,
        busca: String? = null,
        precoMinimo: String? = null,
        precoMaximo: String? = null,
        ordenacao: String = "padrao",
        somenteDisponiveis: Boolean = false,
        somentePromocoes: Boolean = false
    ): CardapioResponse = withContext(Dispatchers.IO) {

        val colecaoAtual = colecao?.trim()
            ?.lowercase(Locale.ROOT).orEmpty()

        val ordenacaoAtual = ordenacao.trim()
            .lowercase(Locale.ROOT).ifEmpty { "padrao" }

        val filtros = FiltrosCardapio(
            busca = busca?.trim(),
            precoMinimo = precoMinimo?.trim(),
            precoMaximo = precoMaximo?.trim(),
            ordenacao = ordenacaoAtual,
            somenteDisponiveis = somenteDisponiveis,
            somentePromocoes = somentePromocoes
        )

        val titulo = when (colecaoAtual) {
            "combos" -> "Combos"
            "promocoes" -> "Promoções"
            "mais-pedidos" -> "Mais pedidos"
            else -> categoriasCarregadas
                .firstOrNull { it.id == categoriaId }
                ?.nome ?: "Todos os produtos"
        }

        val erros = mutableListOf<String>()

        // Validação dos filtros.
        if (colecaoAtual !in setOf(
                "", "combos", "promocoes", "mais-pedidos"
            )
        ) {
            erros.add("Selecione uma coleção válida.")
        }

        if (categoriaId != null && colecaoAtual.isNotEmpty()) {
            erros.add(
                "Escolha uma categoria ou coleção por vez."
            )
        }

        if ((busca?.length ?: 0) > 100) {
            erros.add(
                "A busca deve ter no máximo 100 caracteres."
            )
        }

        if (ordenacaoAtual !in setOf(
                "padrao", "menor-preco",
                "maior-preco", "nome-az", "nome-za"
            )
        ) {
            erros.add("Selecione uma ordenação válida.")
        }

        val minimo = lerPreco(
            precoMinimo, "mínimo", erros
        )

        val maximo = lerPreco(
            precoMaximo, "máximo", erros
        )

        if (minimo != null && maximo != null && minimo > maximo) {
            erros.add(
                "O preço mínimo não pode ser maior que o máximo."
            )
        }

        // A API pública não retorna dados de vendas.
        // Não inventamos um ranking de mais pedidos.
        if (colecaoAtual == "mais-pedidos") {
            erros.add(
                "O ranking de Mais pedidos ainda não está " +
                        "disponível na API pública."
            )
        }

        if (erros.isNotEmpty()) {
            return@withContext CardapioResponse(
                categorias = categoriasCarregadas,
                categoriaSelecionadaId = categoriaId,
                colecaoSelecionada = colecaoAtual.ifEmpty { null },
                titulo = titulo,
                filtros = filtros,
                errosFiltros = erros
            )
        }

        // Consulta os produtos reais do servidor.
        val resposta = api.listarProdutos(categoriaId)

        if (!resposta.isSuccessful) {
            throw Exception(
                extrairMensagemErro(resposta, resposta.errorBody())
            )
        }

        val produtos = resposta.body()
            ?: throw Exception("A API retornou uma resposta vazia.")

        var filtrados = produtos.toList()

        // Filtros das coleções.
        filtrados = when (colecaoAtual) {
            "combos" -> filtrados.filter { it.ehCombo }

            "promocoes" -> filtrados.filter {
                it.ehCombo || it.descontoPercentual > 0.0
            }

            else -> filtrados
        }

        // Busca por nome, ignorando acentos e maiúsculas.
        if (!busca.isNullOrBlank()) {
            val termo = normalizarTexto(busca)

            filtrados = filtrados.filter {
                normalizarTexto(it.nome).contains(termo)
            }
        }

        // Filtros por preço final.
        if (minimo != null) {
            filtrados = filtrados.filter {
                it.preco >= minimo
            }
        }

        if (maximo != null) {
            filtrados = filtrados.filter {
                it.preco <= maximo
            }
        }

        // Filtros por disponibilidade e descontos.
        if (somenteDisponiveis) {
            filtrados = filtrados.filter { it.disponivel }
        }

        if (somentePromocoes) {
            filtrados = filtrados.filter {
                it.descontoPercentual > 0.0
            }
        }

        // Ordenação dos resultados.
        filtrados = when (ordenacaoAtual) {

            "menor-preco" -> filtrados.sortedWith(
                compareBy<Produto> { it.preco }
                    .thenBy { normalizarTexto(it.nome) }
                    .thenBy { it.id }
            )

            "maior-preco" -> filtrados.sortedWith(
                compareByDescending<Produto> { it.preco }
                    .thenBy { normalizarTexto(it.nome) }
                    .thenBy { it.id }
            )

            "nome-za" -> filtrados.sortedWith(
                compareByDescending<Produto> {
                    normalizarTexto(it.nome)
                }.thenBy { it.id }
            )

            else -> filtrados.sortedWith(
                compareBy<Produto> {
                    normalizarTexto(it.nome)
                }.thenBy { it.id }
            )
        }

        // Converte a resposta do site para o modelo
        // que a tela do Android já utiliza.
        CardapioResponse(
            categorias = categoriasCarregadas,
            produtos = filtrados,
            categoriaSelecionadaId = categoriaId,
            colecaoSelecionada = colecaoAtual.ifEmpty { null },
            titulo = titulo,
            filtros = filtros,
            errosFiltros = emptyList()
        )
    }

    // Consulta os detalhes de um produto.
    suspend fun buscarProduto(id: Int): Produto =
        withContext(Dispatchers.IO) {

            val resposta = api.buscarProduto(id)

            if (resposta.isSuccessful && resposta.body() != null) {
                resposta.body()!!
            } else {
                throw Exception(
                    "Não foi possível consultar o produto. " +
                            "Código: ${resposta.code()}"
                )
            }
        }

    // Valida o preço informado pelo usuário.
    private fun lerPreco(
        texto: String?,
        tipo: String,
        erros: MutableList<String>
    ): Double? {

        val valor = texto?.trim().orEmpty()

        if (valor.isEmpty()) return null

        val formato = Regex(
            """\A[0-9]{1,8}(?:[.,][0-9]{1,2})?\z"""
        )

        val numero = if (formato.matches(valor)) {
            valor.replace(',', '.').toDoubleOrNull()
        } else {
            null
        }

        if (numero == null) {
            erros.add(
                "Informe um preço $tipo válido, como 25,90, " +
                        "sem R$ ou separador de milhar."
            )
        }

        return numero
    }

    // Normaliza textos para pesquisa e ordenação.
    private fun normalizarTexto(texto: String): String {
        return Normalizer.normalize(
            texto.trim(),
            Normalizer.Form.NFD
        ).replace(
            Regex("""\p{M}+"""), ""
        ).lowercase(Locale.ROOT)
    }

    // Interpreta mensagens de erro enviadas pelo site.
    private fun extrairMensagemErro(
        resposta: Response<*>,
        corpoErro: ResponseBody?
    ): String {

        val conteudo = corpoErro?.string()

        if (!conteudo.isNullOrBlank()) {
            try {
                val json = JsonParser.parseString(conteudo)
                    .asJsonObject

                val erros = json.getAsJsonArray("errosFiltros")

                if (erros != null && erros.size() > 0) {
                    return erros.joinToString("\n") {
                        it.asString
                    }
                }

                val mensagem = json.get("mensagem")

                if (mensagem != null && !mensagem.isJsonNull) {
                    return mensagem.asString
                }

            } catch (_: Exception) {
                // Mantém a mensagem padrão.
            }
        }

        return "Não foi possível carregar o cardápio. " +
                "Código: ${resposta.code()}"
    }
}