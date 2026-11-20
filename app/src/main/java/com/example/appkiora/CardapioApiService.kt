package com.example.appkiora

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Define as chamadas públicas da API de cardápio do Kiora.
 */
interface CardapioApiService {

    // Consulta as categorias disponíveis.
    @GET("api/cardapio/categorias")
    suspend fun listarCategorias(): Response<List<Categoria>>

    // Recebe a lista de produtos fornecida pelo site.
    // A API atual aceita o filtro de categoria.
    @GET("api/cardapio/produtos")
    suspend fun listarProdutos(
        @Query("categoriaId") categoriaId: Int? = null
    ): Response<List<Produto>>

    // Consulta os detalhes de um produto específico.
    @GET("api/cardapio/produtos/{id}")
    suspend fun buscarProduto(
        @Path("id") id: Int
    ): Response<Produto>
}