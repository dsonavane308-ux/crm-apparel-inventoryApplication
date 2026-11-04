package com.crm.inventory.repository;

import com.crm.inventory.model.PosInvoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PosInvoiceRepository extends JpaRepository<PosInvoice, UUID> {
    List<PosInvoice> findTop50ByOrderByCreatedAtDesc();
}
