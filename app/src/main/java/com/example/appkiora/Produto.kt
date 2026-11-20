package com.example.appkiora

/**
 * Representa um produto do cardápio do Kiora.
 * Mantém a mesma estrutura do ProdutoResponseDTO
 * utilizado pela API do site.
 */
data class Produto(
    val id: Int,
    val nome: String,
    val descricao: String,
    val preco: Double,
    val imagemUrl: String?,
    val disponivel: Boolean,
    val categoriaId: Int,
    val nomeCategoria: String,
    val precoOriginal: Double,
    val descontoPercentual: Double,
    val ehCombo: Boolean
)