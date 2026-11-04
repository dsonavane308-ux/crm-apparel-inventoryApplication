package com.crm.inventory.repository;

import com.crm.inventory.model.StockLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockLevelRepository extends JpaRepository<StockLevel, UUID> {

    Optional<StockLevel> findByItemIdAndWarehouseId(UUID itemId, UUID warehouseId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM StockLevel s WHERE s.itemId = :itemId AND s.warehouseId = :warehouseId")
    Optional<StockLevel> findByItemIdAndWarehouseIdForUpdate(
        @Param("itemId") UUID itemId, 
        @Param("warehouseId") UUID warehouseId
    );

    List<StockLevel> findByItemId(UUID itemId);

    List<StockLevel> findByWarehouseId(UUID warehouseId);
}
