package com.deliveryplatform.order.infrastructure.persistence.repository;

import com.deliveryplatform.order.application.port.out.FechamentoRepositorio;
import com.deliveryplatform.order.domain.model.FechamentoDoDia;
import com.deliveryplatform.order.domain.model.FechamentoDoDia.AcertoDoEntregador;
import com.deliveryplatform.order.infrastructure.persistence.entity.FechamentoJpaEntity;
import com.deliveryplatform.valuetypes.Money;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class FechamentoRepositorioJpa implements FechamentoRepositorio {

    private final FechamentoSpringDataRepository springData;

    public FechamentoRepositorioJpa(FechamentoSpringDataRepository springData) {
        this.springData = springData;
    }

    @Override
    public FechamentoDoDia salvar(FechamentoDoDia f) {
        List<FechamentoJpaEntity.Acerto> acertos = f.getAcertos().stream()
                .map(a -> new FechamentoJpaEntity.Acerto(a.entregadorId(), a.nome(), a.entregas(),
                        a.repasse().valor(), a.dinheiro().valor(), a.cartao().valor()))
                .toList();
        springData.save(new FechamentoJpaEntity(f.getId(), f.getEstabelecimentoId(), f.getDia(),
                f.getFechadoEm(), f.getFechadoPor(), f.getPedidos(), f.getConcluidos(),
                f.getCancelados(), f.getVendas().valor(), f.getTaxas().valor(),
                f.getDinheiro().valor(), f.getCartao().valor(), f.getPix().valor(),
                f.getDinheiroContado() == null ? null : f.getDinheiroContado().valor(),
                f.getObservacao(), acertos));
        return f;
    }

    @Override
    public Optional<FechamentoDoDia> doDia(UUID estabelecimentoId, LocalDate dia) {
        return springData.findByEstabelecimentoIdAndDia(estabelecimentoId, dia)
                .map(FechamentoRepositorioJpa::paraDominio);
    }

    @Override
    public boolean existe(UUID estabelecimentoId, LocalDate dia) {
        return springData.existsByEstabelecimentoIdAndDia(estabelecimentoId, dia);
    }

    @Override
    public List<FechamentoDoDia> recentes(UUID estabelecimentoId, int limite) {
        return springData.findByEstabelecimentoIdOrderByDiaDesc(estabelecimentoId,
                        PageRequest.of(0, limite)).stream()
                .map(FechamentoRepositorioJpa::paraDominio)
                .toList();
    }

    @Override
    public List<FechamentoDoDia> entre(UUID estabelecimentoId, LocalDate de, LocalDate ate) {
        return springData.findByEstabelecimentoIdAndDiaBetweenOrderByDiaDesc(estabelecimentoId, de, ate)
                .stream()
                .map(FechamentoRepositorioJpa::paraDominio)
                .toList();
    }

    private static Money dinheiro(BigDecimal valor) {
        return valor == null ? null : Money.de(valor);
    }

    private static FechamentoDoDia paraDominio(FechamentoJpaEntity e) {
        List<AcertoDoEntregador> acertos = e.getAcertos().stream()
                .map(a -> new AcertoDoEntregador(a.getEntregadorId(), a.getNome(), a.getEntregas(),
                        Money.de(a.getRepasse()), Money.de(a.getDinheiro()), Money.de(a.getCartao())))
                .toList();
        return FechamentoDoDia.reconstituir(e.getId(), e.getEstabelecimentoId(), e.getDia(),
                e.getFechadoEm(), e.getFechadoPor(), e.getPedidos(), e.getConcluidos(),
                e.getCancelados(), Money.de(e.getVendas()), Money.de(e.getTaxas()),
                Money.de(e.getDinheiro()), Money.de(e.getCartao()), Money.de(e.getPix()),
                dinheiro(e.getDinheiroContado()), e.getObservacao(), acertos);
    }
}
