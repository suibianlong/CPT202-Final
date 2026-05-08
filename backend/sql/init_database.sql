SET FOREIGN_KEY_CHECKS = 0;

DROP DATABASE IF EXISTS heritageResourcePlatform;

CREATE DATABASE heritageResourcePlatform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE heritageResourcePlatform;


-- 1. User table

CREATE TABLE `user` (
  `userId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'User ID',
  `username` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Username',
  `email` VARCHAR(255) NOT NULL UNIQUE COMMENT 'Email',
  `passwordHash` VARCHAR(255) NOT NULL COMMENT 'Password hash',
  `role` ENUM('user','reviewer') NOT NULL COMMENT 'Role',
  `isContributor` TINYINT(1) NOT NULL DEFAULT 0 COMMENT 'Whether the user is a contributor: 0=No, 1=Yes',
  `bio` TEXT NULL COMMENT 'User biography',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User table';


-- 2. Category table

CREATE TABLE `category` (
  `categoryId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Category primary key',
  `categoryTopic` VARCHAR(50) NOT NULL COMMENT 'Resource topic',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT 'Usage count',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last management time',
  PRIMARY KEY (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource category table';


-- 3. Resource type table

CREATE TABLE `resourceType` (
  `resourceTypeId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Resource type primary key',
  `typeName` VARCHAR(50) NOT NULL UNIQUE COMMENT 'Resource type name',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT 'Usage count',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`resourceTypeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource type table';


-- 4. Tag table

CREATE TABLE `tag` (
  `tagId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Tag primary key',
  `tagName` VARCHAR(100) NOT NULL UNIQUE COMMENT 'Tag name',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT 'Status: ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT 'Usage count',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`tagId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tag table';

-- =========================
-- 5. Main resource table
-- =========================
CREATE TABLE `resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Resource primary key',
  `contributorId` BIGINT NOT NULL COMMENT 'Contributor ID',
  `title` VARCHAR(255) NOT NULL COMMENT 'Title',
  `description` TEXT NOT NULL COMMENT 'Description',
  `copyright` VARCHAR(500) NOT NULL COMMENT 'Copyright statement',
  `categoryId` BIGINT NOT NULL COMMENT 'Category ID',
  `resourceTypeId` BIGINT NOT NULL COMMENT 'Resource type ID',
  `place` VARCHAR(255) NULL COMMENT 'Place of origin',
  `previewImage` VARCHAR(500) NULL COMMENT 'Preview image URL',
  `mediaUrl` VARCHAR(500) NULL COMMENT 'Resource content URL',
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT 'Status: Draft/Pending Review/Approved/Rejected/Archived',
  `reviewedAt` TIMESTAMP NULL COMMENT 'Review approval time',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `archivedAt` TIMESTAMP NULL COMMENT 'Archive time',
  PRIMARY KEY (`id`),

  KEY `idxContributor` (`contributorId`),
  CONSTRAINT `fkResourceContributor`
    FOREIGN KEY (`contributorId`) REFERENCES `user` (`userId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxCategory` (`categoryId`),
  CONSTRAINT `fkResourceCategory`
    FOREIGN KEY (`categoryId`) REFERENCES `category` (`categoryId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxResourceType` (`resourceTypeId`),
  CONSTRAINT `fkResourceResourceType`
    FOREIGN KEY (`resourceTypeId`) REFERENCES `resourceType` (`resourceTypeId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Main resource table';

-- =========================
-- 6. Archived resource table
-- =========================
CREATE TABLE `resourceArchive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Resource primary key',
  `contributorId` BIGINT NOT NULL COMMENT 'Contributor ID',
  `title` VARCHAR(255) NOT NULL COMMENT 'Title',
  `description` TEXT NOT NULL COMMENT 'Description',
  `copyright` VARCHAR(500) NOT NULL COMMENT 'Copyright statement',
  `categoryId` BIGINT NOT NULL COMMENT 'Category ID',
  `resourceTypeId` BIGINT NOT NULL COMMENT 'Resource type ID',
  `place` VARCHAR(255) NULL COMMENT 'Place of origin',
  `previewImage` VARCHAR(500) NULL COMMENT 'Preview image URL',
  `mediaUrl` VARCHAR(500) NULL COMMENT 'Resource content URL',
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT 'Status: Draft/Pending Review/Approved/Rejected/Archived',
  `reviewedAt` TIMESTAMP NULL COMMENT 'Review approval time',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  `archivedAt` TIMESTAMP NULL COMMENT 'Archive time',
  PRIMARY KEY (`id`),

  KEY `idxContributor` (`contributorId`),
  CONSTRAINT `fkResourceArchiveContributor`
    FOREIGN KEY (`contributorId`) REFERENCES `user` (`userId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxCategory` (`categoryId`),
  CONSTRAINT `fkResourceArchiveCategory`
    FOREIGN KEY (`categoryId`) REFERENCES `category` (`categoryId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxResourceType` (`resourceTypeId`),
  CONSTRAINT `fkResourceArchiveResourceType`
    FOREIGN KEY (`resourceTypeId`) REFERENCES `resourceType` (`resourceTypeId`)
    ON DELETE RESTRICT ON UPDATE CASCADE,

  KEY `idxStatus` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Archived resource table';


-- 7. Resource-tag association table

CREATE TABLE `resourceTag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Association ID',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `tagId` BIGINT NOT NULL COMMENT 'Tag ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqueResourceTag` (`resourceId`, `tagId`),

  CONSTRAINT `fkResourceTagResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkResourceTagTag`
    FOREIGN KEY (`tagId`) REFERENCES `tag` (`tagId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource-tag association table';


-- 8. Resource submission version table

CREATE TABLE `resourceSubmission` (
  `submissionId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Submission version primary key',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `versionNo` INT NOT NULL COMMENT 'Version number, starting from 1',
  `submittedBy` BIGINT NOT NULL COMMENT 'Submitter ID',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
  `submissionNote` TEXT NULL COMMENT 'Submission note',
  `statusSnapshot` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT 'Status at submission',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`submissionId`),

  KEY `idxResource` (`resourceId`),
  CONSTRAINT `fkResourceSubmissionResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkResourceSubmissionUser`
    FOREIGN KEY (`submittedBy`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource submission version table';


-- 9. Review record table

CREATE TABLE `reviewRecord` (
  `reviewRecordId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Review record ID',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `submissionId` BIGINT NOT NULL COMMENT 'Submission version ID',
  `versionNo` INT NOT NULL COMMENT 'Version number',
  `reviewerId` BIGINT NOT NULL COMMENT 'Reviewer ID',
  `actionDescription` VARCHAR(20) NOT NULL COMMENT 'Specific action description',
  `status` ENUM('Approved','Rejected') NOT NULL COMMENT 'Review result',
  `feedbackComment` TEXT NULL COMMENT 'Review comment',
  `reviewedAt` TIMESTAMP NULL COMMENT 'Review time',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Record creation time',
  PRIMARY KEY (`reviewRecordId`),

  KEY `idxResource` (`resourceId`),
  CONSTRAINT `fkReviewRecordResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  KEY `idxSubmission` (`submissionId`),
  CONSTRAINT `fkReviewRecordResourceSubmission`
    FOREIGN KEY (`submissionId`) REFERENCES `resourceSubmission` (`submissionId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkReviewRecordUser`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Review record table';


-- 10. Contributor application table

CREATE TABLE `contributorApplication` (
  `applicationId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Application record primary key',
  `userId` BIGINT NOT NULL COMMENT 'Applicant ID',
  `applicationReason` TEXT NOT NULL COMMENT 'Application reason',
  `approvalStatus` ENUM('PENDING','APPROVED','REJECTED','ARCHIVED') NOT NULL DEFAULT 'PENDING' COMMENT 'Approval status',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
  `reviewerId` BIGINT NULL COMMENT 'Approver ID',
  `reviewedAt` TIMESTAMP NULL COMMENT 'Approval time',
  `reviewComment` TEXT NULL COMMENT 'Approval comment',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`applicationId`),

  KEY `idxUser` (`userId`),
  CONSTRAINT `fkContributorApplicationUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkContributorApplicationReviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Contributor application table';


-- 11. Contributor application archive table

CREATE TABLE `contributorApplicationArchive` (
  `applicationId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Application record primary key',
  `userId` BIGINT NOT NULL COMMENT 'Applicant ID',
  `applicationReason` TEXT NOT NULL COMMENT 'Application reason',
  `approvalStatus` ENUM('PENDING','APPROVED','REJECTED','ARCHIVED') NOT NULL DEFAULT 'PENDING' COMMENT 'Approval status',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Submission time',
  `reviewerId` BIGINT NULL COMMENT 'Approver ID',
  `reviewedAt` TIMESTAMP NULL COMMENT 'Approval time',
  `reviewComment` TEXT NULL COMMENT 'Approval comment',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`applicationId`),

  KEY `idxUser` (`userId`),
  CONSTRAINT `fkContributorApplicationArchiveUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkContributorApplicationArchiveReviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Contributor application archive table';


-- 12. Comment table

CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Comment primary key',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `userId` BIGINT NOT NULL COMMENT 'Comment user ID',
  `content` TEXT NOT NULL COMMENT 'Comment content',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`id`),

  KEY `idxCommentResource` (`resourceId`),
  CONSTRAINT `fkCommentResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  KEY `idxCommentUser` (`userId`),
  CONSTRAINT `fkCommentUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment table';


-- 13. Feedback table

CREATE TABLE `feedback` (
  `feedbackId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Feedback ID',
  `fileNum` INT NOT NULL DEFAULT 0 COMMENT 'Number of attachments',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
  `userId` BIGINT NOT NULL COMMENT 'Uploading user ID',
  `feedbackType` ENUM('Bug Report', 'Suggestion') NOT NULL COMMENT 'Feedback type',
  `description` TEXT NOT NULL COMMENT 'Feedback description',
  PRIMARY KEY (`feedbackId`),

  KEY `idxFeedbackUser` (`userId`),
  CONSTRAINT `fkFeedbackUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `chkFeedbackFileNum`
    CHECK (`fileNum` <= 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback table';


-- 14. Feedback attachment table

CREATE TABLE `attachedFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Attachment primary key',
  `feedbackId` BIGINT NOT NULL COMMENT 'Feedback ID',
  `originalFilename` VARCHAR(500) NOT NULL COMMENT 'Original filename',
  `storedFilename` VARCHAR(50) NOT NULL COMMENT 'Stored filename',
  `filePath` VARCHAR(500) NOT NULL COMMENT 'File path/URL',
  `fileType` ENUM('JPG','PNG','PDF','TXT') NOT NULL COMMENT 'File type',
  `fileSize` BIGINT NOT NULL COMMENT 'File size',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
  PRIMARY KEY (`fileId`),

  KEY `idxAttachedFileFeedback` (`feedbackId`),
  CONSTRAINT `fkAttachedFileFeedback`
    FOREIGN KEY (`feedbackId`) REFERENCES `feedback` (`feedbackId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `chkAttachedFileSize`
    CHECK (`fileSize` <= 10485760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Feedback attachment table';


-- 15. Resource history version table

CREATE TABLE `resourceVersion` (
  `versionId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Version record primary key',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `versionNo` INT NOT NULL COMMENT 'Internal resource version number',
  `snapshot` LONGTEXT NOT NULL COMMENT 'Resource snapshot JSON',
  `changeType` VARCHAR(50) NOT NULL COMMENT 'Change type: create/edit/submit/rollback/revision',
  `changeSummary` VARCHAR(500) NULL COMMENT 'Change summary',
  `createdBy` BIGINT NOT NULL COMMENT 'Version creator ID',
  `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Version creation time',
  PRIMARY KEY (`versionId`),

  UNIQUE KEY `ukResourceVersionResourceVersionNo` (`resourceId`, `versionNo`),
  KEY `idxResourceVersionResourceId` (`resourceId`),
  KEY `idxResourceVersionCreatedBy` (`createdBy`),

  CONSTRAINT `fkResourceVersionResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkResourceVersionUser`
    FOREIGN KEY (`createdBy`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource history version table';


-- 16. Resource attachment table

CREATE TABLE `resourceFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Resource attachment primary key',
  `resourceId` BIGINT NOT NULL COMMENT 'Resource ID',
  `originalFilename` VARCHAR(500) NOT NULL COMMENT 'Original filename',
  `storedFilename` VARCHAR(50) NOT NULL COMMENT 'Stored filename',
  `filePath` VARCHAR(500) NOT NULL COMMENT 'File path/URL',
  `fileType` VARCHAR(50) NOT NULL COMMENT 'File type',
  `fileSize` BIGINT NOT NULL COMMENT 'File size',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Upload time',
  PRIMARY KEY (`fileId`),

  KEY `idxResourceFileResourceId` (`resourceId`),
  CONSTRAINT `fkResourceFileResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Resource attachment table';


-- 17. New database-based Admin History

CREATE TABLE `adminOperationHistory` (
  `historyId` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'History record primary key',
  `itemName` VARCHAR(255) NOT NULL COMMENT 'Item name',
  `kind` VARCHAR(50) NOT NULL COMMENT 'Category kind, such as Type / Topic / Tag / Classification',
  `module` VARCHAR(50) NOT NULL COMMENT 'Module, such as classification / tag',
  `action` VARCHAR(50) NOT NULL COMMENT 'Action, such as Created / Updated / Activated / Deactivated / Saved',
  `administrator` VARCHAR(100) NOT NULL COMMENT 'Administrator name',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Creation time',
  PRIMARY KEY (`historyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Administrator operation history table';


-- user test data
INSERT INTO `user` (`username`, `email`, `passwordHash`, `role`, `isContributor`, `bio`)
VALUES
('alice', 'alice@example.com', '120000:61d57547c7564253a3ea78374825595a:c9b15df730950a4983421d5f4f2ab65999f95ee3b1add083a697b1865a2aee3c', 'user', 0, 'test user'),
('bob_contributor', 'bob_contributor@example.com', '120000:1ea4032cd7420d9728a1218ae80c718c:99f4b27f3ec8aa57f845e6e2fd763c437d7b744dabf4617d485d827f03ad9346', 'user', 1, 'test contributor user'),
('carol_viewer', 'carol_viewer@example.com', '120000:e58617416de9080e17459ab6e024b50f:52cd0d29031068f409abffac1ec50a2ce5d6d1fce598949b1999abf242c0acf3', 'user', 0, 'test viewer user'),
('david_reviewer', 'david_reviewer@example.com', '120000:11b985b53f43ba0e670aa8a66a722eb8:723fabc568aaec67a9996decadbe2da292c9f46da01a1bee0d62c84914d2b539', 'reviewer', 0, 'test reviewer user');

-- Default resource types
INSERT INTO `resourceType` (`typeName`, `status`, `usageCount`)
VALUES
('photo', 'ACTIVE', 0),
('video', 'ACTIVE', 0),
('audio', 'ACTIVE', 0),
('document', 'ACTIVE', 0),
('extra link', 'ACTIVE', 0),
('other', 'ACTIVE', 0);

-- Default resource topics
INSERT INTO `category` (`categoryTopic`, `status`, `usageCount`)
VALUES
('places', 'ACTIVE', 0),
('traditions', 'ACTIVE', 0),
('stories', 'ACTIVE', 0),
('objects', 'ACTIVE', 0),
('educational materials', 'ACTIVE', 0),
('other', 'ACTIVE', 0);

SET FOREIGN_KEY_CHECKS = 1;
