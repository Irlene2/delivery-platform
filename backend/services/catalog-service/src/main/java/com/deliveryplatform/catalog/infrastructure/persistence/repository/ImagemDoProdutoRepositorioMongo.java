package com.deliveryplatform.catalog.infrastructure.persistence.repository;

import com.deliveryplatform.catalog.application.port.out.ImagemDoProdutoRepositorio;
import com.deliveryplatform.catalog.domain.model.ImagemDoProduto;
import com.deliveryplatform.catalog.infrastructure.persistence.entity.ImagemDocumento;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ImagemDoProdutoRepositorioMongo implements ImagemDoProdutoRepositorio {

    private final ImagemSpringDataRepository documentos;

    public ImagemDoProdutoRepositorioMongo(ImagemSpringDataRepository documentos) {
        this.documentos = documentos;
    }

    @Override
    public void salvar(ImagemDoProduto imagem) {
        documentos.save(new ImagemDocumento(imagem.produtoId(), imagem.estabelecimentoId(),
                imagem.tipo(), imagem.conteudo()));
    }

    @Override
    public Optional<ImagemDoProduto> buscar(UUID produtoId) {
        return documentos.findById(produtoId)
                .map(d -> new ImagemDoProduto(d.produtoId(), d.estabelecimentoId(), d.tipo(),
                        d.conteudo()));
    }
}
