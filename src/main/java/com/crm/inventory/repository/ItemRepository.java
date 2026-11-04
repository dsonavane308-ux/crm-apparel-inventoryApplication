package com.crm.inventory.repository;

import com.crm.inventory.model.Item;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ItemRepository extends JpaRepository<Item, UUID> {

    Optional<Item> findBySku(String sku);

    Optional<Item> findByBarcode(String barcode);

    boolean existsBySku(String sku);

    Page<Item> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(
            String name, String sku, String barcode, Pageable pageable);

    @Query("""
        SELECT i FROM Item i 
        WHERE (SELECT COALESCE(SUM(s.quantityOnHand - s.quantityReserved), 0) 
               FROM StockLevel s WHERE s.itemId = i.id) <= i.reorderThreshold 
          AND i.status = 'ACTIVE'
    """)
    List<Item> findLowStockItems();
}
