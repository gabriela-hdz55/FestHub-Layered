package com.festhub.backend.repository;

import com.festhub.backend.model.PromoMedia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoMediaRepository {

    PromoMedia save(PromoMedia promoMedia);

    Optional<PromoMedia> findById(UUID id);

    List<PromoMedia> findAll();

    Optional<PromoMedia> update(PromoMedia promoMedia);

    boolean deleteById(UUID id);
}
