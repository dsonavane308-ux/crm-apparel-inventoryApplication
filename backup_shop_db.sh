#!/bin/bash
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="backups/clothing_shop_backup_${TIMESTAMP}.sql"
docker exec crm_inventory_db pg_dump -U postgres inventory_db > "${BACKUP_FILE}"
echo "Offline database backup saved to: ${BACKUP_FILE}"
