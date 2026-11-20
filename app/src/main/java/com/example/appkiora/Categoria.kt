package com.example.appkiora

/**
 * Representa uma categoria do cardápio do Kiora.
 * Mantém a mesma estrutura do CategoriaResponseDTO do site.
 */
data class Categoria(
    val id: Int,
    val nome: String
)