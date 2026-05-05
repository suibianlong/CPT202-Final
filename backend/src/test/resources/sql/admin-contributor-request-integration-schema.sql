CREATE TABLE IF NOT EXISTS `user` (
  `userId` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(100) NOT NULL,
  `email` VARCHAR(255) NOT NULL,
  `passwordHash` VARCHAR(255) NOT NULL,
  `role` ENUM('user','reviewer') NOT NULL,
  `isContributor` TINYINT(1) NOT NULL DEFAULT 0,
  `bio` TEXT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`userId`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `contributorApplication` (
  `applicationId` BIGINT NOT NULL AUTO_INCREMENT,
  `userId` BIGINT NOT NULL,
  `applicationReason` TEXT NOT NULL,
  `approvalStatus` ENUM('PENDING','APPROVED','REJECTED','ARCHIVED') NOT NULL DEFAULT 'PENDING',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `reviewerId` BIGINT NULL,
  `reviewedAt` TIMESTAMP NULL,
  `reviewComment` TEXT NULL,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`applicationId`),
  KEY `idx_contributor_application_user` (`userId`),
  KEY `idx_contributor_application_reviewer` (`reviewerId`),
  CONSTRAINT `fk_contributor_application_user`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_contributor_application_reviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `adminOperationHistory` (
  `historyId` BIGINT NOT NULL AUTO_INCREMENT,
  `itemName` VARCHAR(255) NOT NULL,
  `kind` VARCHAR(50) NOT NULL,
  `module` VARCHAR(50) NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `administrator` VARCHAR(100) NOT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`historyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
