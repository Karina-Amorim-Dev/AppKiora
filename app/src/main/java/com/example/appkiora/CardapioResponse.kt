package com.example.appkiora

/**
 * Representa a resposta completa da API de cardápio.
 */
data class CardapioResponse(
    val categorias: List<Categoria> = emptyList(),
    val produtos: List<Produto> = emptyList(),
    val categoriaSelecionadaId: Int? = null,
    val colecaoSelecionada: String? = null,
    val titulo: String = "Todos os produtos",
    val filtros: FiltrosCardapio = FiltrosCardapio(),
    val errosFiltros: List<String> = emptyList(),
    val periodoMaisPedidos: String? = null
)

/**
 * Representa os filtros utilizados na consulta.
 */
data class FiltrosCardapio(
    val busca: String? = null,
    val precoMinimo: String? = null,
    val precoMaximo: String? = null,
    val ordenacao: String = "padrao",
    val somenteDisponiveis: Boolean = false,
    val somentePromocoes: Boolean = false
)