CREATE TABLE `sectors` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `crckey` varchar(30) NOT NULL DEFAULT '',
  `category` varchar(6) NOT NULL DEFAULT '',
  `code` varchar(20) NOT NULL DEFAULT '',
  `name` varchar(60) NOT NULL DEFAULT '',
  PRIMARY KEY (`id`),
  KEY `Sectors_code_idx` (`code`) USING BTREE,
  KEY `Sectors_category_idx` (`category`) USING BTREE
) ENGINE=InnoDB   DEFAULT CHARSET=utf8;


CREATE TABLE `sector_secs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `weight` float NOT NULL,
  `sectors_id` bigint(20) NOT NULL,
  `secs_id` bigint(20) NOT NULL,
  `validTo` bigint(20) NOT NULL,
  `validFrom` bigint(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `SectorSecs_sector_idx` (`sectors_id`) USING BTREE,
  KEY `SectorSecs_sec_idx` (`secs_id`) USING BTREE,
  KEY `SectorSecs_validFrom_idx` (`validFrom`) USING BTREE,
  KEY `SectorSecs_validToFrom_idx` (`validTo`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `money_flows1d` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `secs_id` bigint(20) NOT NULL,
  `time` bigint(20) NOT NULL,
  `totalVolume` double NOT NULL,
  `totalAmount` double NOT NULL,
  `totalVolumeIn` double NOT NULL,    
 `totalAmountIn` double NOT NULL, 
 `totalVolumeOut` double NOT NULL, 
 `totalAmountOut` double NOT NULL, 
 `totalVolumeEven` double NOT NULL, 
 `totalAmountEven` double NOT NULL, 
  `superVolume` double NOT NULL,
  `superAmount` double NOT NULL,
  `superVolumeIn` double NOT NULL,    
 `superAmountIn` double NOT NULL, 
 `superVolumeOut` double NOT NULL, 
 `superAmountOut` double NOT NULL, 
 `superVolumeEven` double NOT NULL, 
 `superAmountEven` double NOT NULL, 
  `largeVolume` double NOT NULL,
  `largeAmount` double NOT NULL,
  `largeVolumeIn` double NOT NULL,    
 `largeAmountIn` double NOT NULL, 
 `largeVolumeOut` double NOT NULL, 
 `largeAmountOut` double NOT NULL, 
 `largeVolumeEven` double NOT NULL, 
 `largeAmountEven` double NOT NULL, 
  `smallVolume` double NOT NULL,
  `smallAmount` double NOT NULL,
 `smallVolumeIn` double NOT NULL,    
 `smallAmountIn` double NOT NULL, 
 `smallVolumeOut` double NOT NULL, 
 `smallAmountOut` double NOT NULL, 
 `smallVolumeEven` double NOT NULL, 
 `smallAmountEven` double NOT NULL, 
  `flag` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `money_flows1d_new_secs_id_fkey` (`secs_id`),
  CONSTRAINT `money_flows1d_new_secs_id_fkey` FOREIGN KEY (`secs_id`) REFERENCES `secs` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 ;

CREATE TABLE money_flows1m (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  secs_id bigint(20) NOT NULL,
  time bigint(20) NOT NULL,
  totalVolume double NOT NULL,
  totalAmount double NOT NULL,
  totalVolumeIn double NOT NULL,    
 totalAmountIn double NOT NULL, 
 totalVolumeOut double NOT NULL, 
 totalAmountOut double NOT NULL, 
 totalVolumeEven double NOT NULL, 
 totalAmountEven double NOT NULL, 
  superVolume double NOT NULL,
  superAmount double NOT NULL,
  superVolumeIn double NOT NULL,    
 superAmountIn double NOT NULL, 
 superVolumeOut double NOT NULL, 
 superAmountOut double NOT NULL, 
 superVolumeEven double NOT NULL, 
 superAmountEven double NOT NULL, 
  largeVolume double NOT NULL,
  largeAmount double NOT NULL,
  largeVolumeIn double NOT NULL,    
 largeAmountIn double NOT NULL, 
 largeVolumeOut double NOT NULL, 
 largeAmountOut double NOT NULL, 
 largeVolumeEven double NOT NULL, 
 largeAmountEven double NOT NULL, 
  smallVolume double NOT NULL,
  smallAmount double NOT NULL,
 smallVolumeIn double NOT NULL,    
 smallAmountIn double NOT NULL, 
 smallVolumeOut double NOT NULL, 
 smallAmountOut double NOT NULL, 
 smallVolumeEven double NOT NULL, 
 smallAmountEven double NOT NULL, 
  flag int(11) NOT NULL,
  PRIMARY KEY (secs_id,time),
  KEY id (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 
PARTITION BY KEY (secs_id)
PARTITIONS 800;