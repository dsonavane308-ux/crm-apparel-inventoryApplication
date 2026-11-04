package com.crm.inventory.repository;

import com.crm.inventory.model.StockTransaction;
import com.crm.inventory.model.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, UUID> {

    Page<StockTransaction> findByItemId(UUID itemId, Pageable pageable);

    Page<StockTransaction> findByReferenceId(String referenceId, Pageable pageable);

    Page<StockTransaction> findByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end, Pageable pageable);

    Page<StockTransaction> findByType(TransactionType type, Pageable pageable);
}
