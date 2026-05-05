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

CREATE TABLE IF NOT EXISTS `category` (
  `categoryId` BIGINT NOT NULL AUTO_INCREMENT,
  `categoryTopic` VARCHAR(50) NOT NULL,
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceType` (
  `resourceTypeId` BIGINT NOT NULL AUTO_INCREMENT,
  `typeName` VARCHAR(50) NOT NULL,
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`resourceTypeId`),
  UNIQUE KEY `uk_resource_type_name` (`typeName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tag` (
  `tagId` BIGINT NOT NULL AUTO_INCREMENT,
  `tagName` VARCHAR(100) NOT NULL,
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`tagId`),
  UNIQUE KEY `uk_tag_name` (`tagName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contributorId` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NOT NULL,
  `copyright` VARCHAR(500) NOT NULL,
  `categoryId` BIGINT NOT NULL,
  `resourceTypeId` BIGINT NOT NULL,
  `place` VARCHAR(255) NULL,
  `previewImage` VARCHAR(500) NULL,
  `mediaUrl` VARCHAR(500) NULL,
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL,
  `reviewedAt` TIMESTAMP NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `archivedAt` TIMESTAMP NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_contributor` (`contributorId`),
  KEY `idx_resource_category` (`categoryId`),
  KEY `idx_resource_type` (`resourceTypeId`),
  CONSTRAINT `fk_resource_contributor`
    FOREIGN KEY (`contributorId`) REFERENCES `user` (`userId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_category`
    FOREIGN KEY (`categoryId`) REFERENCES `category` (`categoryId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_type`
    FOREIGN KEY (`resourceTypeId`) REFERENCES `resourceType` (`resourceTypeId`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceArchive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `contributorId` BIGINT NOT NULL,
  `title` VARCHAR(255) NOT NULL,
  `description` TEXT NOT NULL,
  `copyright` VARCHAR(500) NOT NULL,
  `categoryId` BIGINT NOT NULL,
  `resourceTypeId` BIGINT NOT NULL,
  `place` VARCHAR(255) NULL,
  `previewImage` VARCHAR(500) NULL,
  `mediaUrl` VARCHAR(500) NULL,
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL,
  `reviewedAt` TIMESTAMP NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `archivedAt` TIMESTAMP NULL,
  PRIMARY KEY (`id`),
  KEY `idx_resource_archive_contributor` (`contributorId`),
  KEY `idx_resource_archive_category` (`categoryId`),
  KEY `idx_resource_archive_type` (`resourceTypeId`),
  CONSTRAINT `fk_resource_archive_contributor`
    FOREIGN KEY (`contributorId`) REFERENCES `user` (`userId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_archive_category`
    FOREIGN KEY (`categoryId`) REFERENCES `category` (`categoryId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_archive_type`
    FOREIGN KEY (`resourceTypeId`) REFERENCES `resourceType` (`resourceTypeId`)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceTag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `tagId` BIGINT NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resource_tag` (`resourceId`, `tagId`),
  KEY `idx_resource_tag_tag` (`tagId`),
  CONSTRAINT `fk_resource_tag_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_tag_tag`
    FOREIGN KEY (`tagId`) REFERENCES `tag` (`tagId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceSubmission` (
  `submissionId` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `versionNo` INT NOT NULL,
  `submittedBy` BIGINT NOT NULL,
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `submissionNote` TEXT NULL,
  `statusSnapshot` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`submissionId`),
  KEY `idx_resource_submission_resource` (`resourceId`),
  KEY `idx_resource_submission_user` (`submittedBy`),
  CONSTRAINT `fk_resource_submission_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_submission_user`
    FOREIGN KEY (`submittedBy`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `reviewRecord` (
  `reviewRecordId` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `submissionId` BIGINT NOT NULL,
  `versionNo` INT NOT NULL,
  `reviewerId` BIGINT NOT NULL,
  `actionDescription` VARCHAR(20) NOT NULL,
  `status` ENUM('Approved','Rejected') NOT NULL,
  `feedbackComment` TEXT NULL,
  `reviewedAt` TIMESTAMP NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`reviewRecordId`),
  KEY `idx_review_record_resource` (`resourceId`),
  KEY `idx_review_record_submission` (`submissionId`),
  KEY `idx_review_record_reviewer` (`reviewerId`),
  CONSTRAINT `fk_review_record_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_record_submission`
    FOREIGN KEY (`submissionId`) REFERENCES `resourceSubmission` (`submissionId`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_review_record_reviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
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

CREATE TABLE IF NOT EXISTS `contributorApplicationArchive` (
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
  KEY `idx_contributor_application_archive_user` (`userId`),
  KEY `idx_contributor_application_archive_reviewer` (`reviewerId`),
  CONSTRAINT `fk_contributor_application_archive_user`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_contributor_application_archive_reviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `userId` BIGINT NOT NULL,
  `content` TEXT NOT NULL,
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_resource` (`resourceId`),
  KEY `idx_comment_user` (`userId`),
  CONSTRAINT `fk_comment_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_comment_user`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `feedback` (
  `feedbackId` BIGINT NOT NULL AUTO_INCREMENT,
  `fileNum` INT NOT NULL DEFAULT 0,
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `userId` BIGINT NOT NULL,
  `feedbackType` ENUM('Bug Report', 'Suggestion') NOT NULL,
  `description` TEXT NOT NULL,
  PRIMARY KEY (`feedbackId`),
  KEY `idx_feedback_user` (`userId`),
  CONSTRAINT `fk_feedback_user`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `attachedFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT,
  `feedbackId` BIGINT NOT NULL,
  `originalFilename` VARCHAR(500) NOT NULL,
  `storedFilename` VARCHAR(50) NOT NULL,
  `filePath` VARCHAR(500) NOT NULL,
  `fileType` ENUM('JPG','PNG','PDF','TXT') NOT NULL,
  `fileSize` BIGINT NOT NULL,
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileId`),
  KEY `idx_attached_file_feedback` (`feedbackId`),
  CONSTRAINT `fk_attached_file_feedback`
    FOREIGN KEY (`feedbackId`) REFERENCES `feedback` (`feedbackId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceVersion` (
  `versionId` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `versionNo` INT NOT NULL,
  `snapshot` LONGTEXT NOT NULL,
  `changeType` VARCHAR(50) NOT NULL,
  `changeSummary` VARCHAR(500) NULL,
  `createdBy` BIGINT NOT NULL,
  `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`versionId`),
  UNIQUE KEY `uk_resource_version` (`resourceId`, `versionNo`),
  KEY `idx_resource_version_resource` (`resourceId`),
  KEY `idx_resource_version_created_by` (`createdBy`),
  CONSTRAINT `fk_resource_version_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_resource_version_user`
    FOREIGN KEY (`createdBy`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `resourceFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT,
  `resourceId` BIGINT NOT NULL,
  `originalFilename` VARCHAR(500) NOT NULL,
  `storedFilename` VARCHAR(50) NOT NULL,
  `filePath` VARCHAR(500) NOT NULL,
  `fileType` VARCHAR(50) NOT NULL,
  `fileSize` BIGINT NOT NULL,
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`fileId`),
  KEY `idx_resource_file_resource` (`resourceId`),
  CONSTRAINT `fk_resource_file_resource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
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
