SET FOREIGN_KEY_CHECKS = 0;

-- 如果数据库已存在则先删除
DROP DATABASE IF EXISTS heritageResourcePlatform;

-- 创建数据库
CREATE DATABASE heritageResourcePlatform CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE heritageResourcePlatform;

-- =========================
-- 1. 用户表
-- =========================
CREATE TABLE `user` (
  `userId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名',
  `email` VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱',
  `passwordHash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `role` ENUM('user','reviewer') NOT NULL COMMENT '角色',
  `isContributor` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为贡献者 0=否 1=是',
  `bio` TEXT NULL COMMENT '个人简介',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`userId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- =========================
-- 2. 分类表
-- =========================
CREATE TABLE `category` (
  `categoryId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类主键',
  `categoryTopic` VARCHAR(50) NOT NULL COMMENT '资源主题',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后管理时间',
  PRIMARY KEY (`categoryId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源分类表';

-- =========================
-- 3. 资源类型表
-- =========================
CREATE TABLE `resourceType` (
  `resourceTypeId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源类型主键',
  `typeName` VARCHAR(50) NOT NULL UNIQUE COMMENT '资源类型名称',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`resourceTypeId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源类型表';

-- =========================
-- 4. 标签表
-- =========================
CREATE TABLE `tag` (
  `tagId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签主键',
  `tagName` VARCHAR(100) NOT NULL UNIQUE COMMENT '标签名',
  `status` ENUM('ACTIVE','INACTIVE') NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
  `usageCount` INT NOT NULL DEFAULT 0 COMMENT '使用次数',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `lastUpdatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`tagId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- =========================
-- 5. 资源主表
-- =========================
CREATE TABLE `resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源主键',
  `contributorId` BIGINT NOT NULL COMMENT '贡献者ID',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `description` TEXT NOT NULL COMMENT '描述',
  `copyright` VARCHAR(500) NOT NULL COMMENT '版权声明',
  `categoryId` BIGINT NOT NULL COMMENT '分类ID',
  `resourceTypeId` BIGINT NOT NULL COMMENT '资源类型ID',
  `place` VARCHAR(255) NULL COMMENT '归属地',
  `previewImage` VARCHAR(500) NULL COMMENT '预览图URL',
  `mediaUrl` VARCHAR(500) NULL COMMENT '资源内容URL',
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT '状态：Draft/Pending Review/Approved/Rejected/Archived',
  `reviewedAt` TIMESTAMP NULL COMMENT '审核通过时间',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `archivedAt` TIMESTAMP NULL COMMENT '归档时间',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源主表';

-- =========================
-- 6. 归档资源表
-- =========================
CREATE TABLE `resourceArchive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源主键',
  `contributorId` BIGINT NOT NULL COMMENT '贡献者ID',
  `title` VARCHAR(255) NOT NULL COMMENT '标题',
  `description` TEXT NOT NULL COMMENT '描述',
  `copyright` VARCHAR(500) NOT NULL COMMENT '版权声明',
  `categoryId` BIGINT NOT NULL COMMENT '分类ID',
  `resourceTypeId` BIGINT NOT NULL COMMENT '资源类型ID',
  `place` VARCHAR(255) NULL COMMENT '归属地',
  `previewImage` VARCHAR(500) NULL COMMENT '预览图URL',
  `mediaUrl` VARCHAR(500) NULL COMMENT '资源内容URL',
  `status` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT '状态：Draft/Pending Review/Approved/Rejected/Archived',
  `reviewedAt` TIMESTAMP NULL COMMENT '审核通过时间',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `archivedAt` TIMESTAMP NULL COMMENT '归档时间',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档资源表';

-- =========================
-- 7. 资源标签关联表
-- =========================
CREATE TABLE `resourceTag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `tagId` BIGINT NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uniqueResourceTag` (`resourceId`, `tagId`),

  CONSTRAINT `fkResourceTagResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkResourceTagTag`
    FOREIGN KEY (`tagId`) REFERENCES `tag` (`tagId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源标签关联表';

-- =========================
-- 8. 资源提交版本表
-- =========================
CREATE TABLE `resourceSubmission` (
  `submissionId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '提交版本主键',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `versionNo` INT NOT NULL COMMENT '版本号，从1开始',
  `submittedBy` BIGINT NOT NULL COMMENT '提交人ID',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `submissionNote` TEXT NULL COMMENT '提交说明',
  `statusSnapshot` ENUM('Draft','Pending Review','Approved','Rejected','Archived') NOT NULL COMMENT '提交时状态',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`submissionId`),

  KEY `idxResource` (`resourceId`),
  CONSTRAINT `fkResourceSubmissionResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkResourceSubmissionUser`
    FOREIGN KEY (`submittedBy`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源提交版本表';

-- =========================
-- 9. 审核记录表
-- =========================
CREATE TABLE `reviewRecord` (
  `reviewRecordId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审核记录ID',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `submissionId` BIGINT NOT NULL COMMENT '提交版本ID',
  `versionNo` INT NOT NULL COMMENT '版本号',
  `reviewerId` BIGINT NOT NULL COMMENT '审核人ID',
  `actionDescription` VARCHAR(20) NOT NULL COMMENT '具体行为描述',
  `status` ENUM('Approved','Rejected') NOT NULL COMMENT '审核结果',
  `feedbackComment` TEXT NULL COMMENT '审核意见',
  `reviewedAt` TIMESTAMP NULL COMMENT '审核时间',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审核记录表';

-- =========================
-- 10. 贡献者申请表
-- =========================
CREATE TABLE `contributorApplication` (
  `applicationId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请记录主键',
  `userId` BIGINT NOT NULL COMMENT '申请人ID',
  `applicationReason` TEXT NOT NULL COMMENT '申请理由',
  `approvalStatus` ENUM('PENDING','APPROVED','REJECTED','ARCHIVED') NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewerId` BIGINT NULL COMMENT '审批人ID',
  `reviewedAt` TIMESTAMP NULL COMMENT '审批时间',
  `reviewComment` TEXT NULL COMMENT '审批意见',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`applicationId`),

  KEY `idxUser` (`userId`),
  CONSTRAINT `fkContributorApplicationUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkContributorApplicationReviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贡献者申请表';

-- =========================
-- 11. 贡献者申请归档表
-- =========================
CREATE TABLE `contributorApplicationArchive` (
  `applicationId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '申请记录主键',
  `userId` BIGINT NOT NULL COMMENT '申请人ID',
  `applicationReason` TEXT NOT NULL COMMENT '申请理由',
  `approvalStatus` ENUM('PENDING','APPROVED','REJECTED','ARCHIVED') NOT NULL DEFAULT 'PENDING' COMMENT '审批状态',
  `submittedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `reviewerId` BIGINT NULL COMMENT '审批人ID',
  `reviewedAt` TIMESTAMP NULL COMMENT '审批时间',
  `reviewComment` TEXT NULL COMMENT '审批意见',
  `updatedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`applicationId`),

  KEY `idxUser` (`userId`),
  CONSTRAINT `fkContributorApplicationArchiveUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `fkContributorApplicationArchiveReviewer`
    FOREIGN KEY (`reviewerId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='贡献者申请归档表';

-- =========================
-- 12. 评论表
-- =========================
CREATE TABLE `comment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论主键',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `userId` BIGINT NOT NULL COMMENT '评论用户ID',
  `content` TEXT NOT NULL COMMENT '评论内容',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),

  KEY `idxCommentResource` (`resourceId`),
  CONSTRAINT `fkCommentResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  KEY `idxCommentUser` (`userId`),
  CONSTRAINT `fkCommentUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- =========================
-- 13. 反馈表
-- =========================
CREATE TABLE `feedback` (
  `feedbackId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `fileNum` INT NOT NULL DEFAULT 0 COMMENT '附件数量',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `userId` BIGINT NOT NULL COMMENT '上传用户ID',
  `feedbackType` ENUM('Bug Report', 'Suggestion') NOT NULL COMMENT '反馈类型',
  `description` TEXT NOT NULL COMMENT '反馈描述',
  PRIMARY KEY (`feedbackId`),

  KEY `idxFeedbackUser` (`userId`),
  CONSTRAINT `fkFeedbackUser`
    FOREIGN KEY (`userId`) REFERENCES `user` (`userId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `chkFeedbackFileNum`
    CHECK (`fileNum` <= 3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈表';

-- =========================
-- 14. 反馈附件表
-- =========================
CREATE TABLE `attachedFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '附件主键',
  `feedbackId` BIGINT NOT NULL COMMENT '反馈ID',
  `originalFilename` VARCHAR(500) NOT NULL COMMENT '原文件名',
  `storedFilename` VARCHAR(50) NOT NULL COMMENT '存储文件名',
  `filePath` VARCHAR(500) NOT NULL COMMENT '文件路径/URL',
  `fileType` ENUM('JPG','PNG','PDF','TXT') NOT NULL COMMENT '文件类型',
  `fileSize` BIGINT NOT NULL COMMENT '文件大小',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`fileId`),

  KEY `idxAttachedFileFeedback` (`feedbackId`),
  CONSTRAINT `fkAttachedFileFeedback`
    FOREIGN KEY (`feedbackId`) REFERENCES `feedback` (`feedbackId`)
    ON DELETE CASCADE ON UPDATE CASCADE,

  CONSTRAINT `chkAttachedFileSize`
    CHECK (`fileSize` <= 10485760)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='反馈附件表';

-- =========================
-- 15. 资源历史版本表
-- =========================
CREATE TABLE `resourceVersion` (
  `versionId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '版本记录主键',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `versionNo` INT NOT NULL COMMENT '资源内部版本号',
  `snapshot` LONGTEXT NOT NULL COMMENT '资源快照JSON',
  `changeType` VARCHAR(50) NOT NULL COMMENT '变更类型 create/edit/submit/rollback/revision',
  `changeSummary` VARCHAR(500) NULL COMMENT '变更摘要',
  `createdBy` BIGINT NOT NULL COMMENT '版本创建人ID',
  `createdAt` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '版本创建时间',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源历史版本表';

-- =========================
-- 16. 资源附件表
-- =========================
CREATE TABLE `resourceFile` (
  `fileId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '资源附件主键',
  `resourceId` BIGINT NOT NULL COMMENT '资源ID',
  `originalFilename` VARCHAR(500) NOT NULL COMMENT '原文件名',
  `storedFilename` VARCHAR(50) NOT NULL COMMENT '存储文件名',
  `filePath` VARCHAR(500) NOT NULL COMMENT '文件路径/URL',
  `fileType` VARCHAR(50) NOT NULL COMMENT '文件类型',
  `fileSize` BIGINT NOT NULL COMMENT '文件大小',
  `uploadedAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`fileId`),

  KEY `idxResourceFileResourceId` (`resourceId`),
  CONSTRAINT `fkResourceFileResource`
    FOREIGN KEY (`resourceId`) REFERENCES `resource` (`id`)
    ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资源附件表';

-- =========================
-- 17. 新加的数据库版 Admin History
-- =========================
CREATE TABLE `adminOperationHistory` (
  `historyId` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录主键',
  `itemName` VARCHAR(255) NOT NULL COMMENT '项目名称',
  `kind` VARCHAR(50) NOT NULL COMMENT '分类种类，如 Type / Topic / Tag / Classification',
  `module` VARCHAR(50) NOT NULL COMMENT '模块，如 classification / tag',
  `action` VARCHAR(50) NOT NULL COMMENT '动作，如 Created / Updated / Activated / Deactivated / Saved',
  `administrator` VARCHAR(100) NOT NULL COMMENT '管理员名称',
  `createdAt` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`historyId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作历史表';

-- =========================
-- 默认初始化数据
-- =========================

-- user 测试数据
-- 正式 role 只有 user / reviewer，contributor 通过 isContributor = 1 表示
INSERT INTO `user` (`username`, `email`, `passwordHash`, `role`, `isContributor`, `bio`)
VALUES
('alice', 'alice@example.com', '120000:61d57547c7564253a3ea78374825595a:c9b15df730950a4983421d5f4f2ab65999f95ee3b1add083a697b1865a2aee3c', 'user', 0, 'test user'),
('bob_contributor', 'bob_contributor@example.com', '120000:1ea4032cd7420d9728a1218ae80c718c:99f4b27f3ec8aa57f845e6e2fd763c437d7b744dabf4617d485d827f03ad9346', 'user', 1, 'test contributor user'),
('carol_viewer', 'carol_viewer@example.com', '120000:e58617416de9080e17459ab6e024b50f:52cd0d29031068f409abffac1ec50a2ce5d6d1fce598949b1999abf242c0acf3', 'user', 0, 'test viewer user'),
('david_reviewer', 'david_reviewer@example.com', '120000:11b985b53f43ba0e670aa8a66a722eb8:723fabc568aaec67a9996decadbe2da292c9f46da01a1bee0d62c84914d2b539', 'reviewer', 0, 'test reviewer user');

-- 默认资源类型
INSERT INTO `resourceType` (`typeName`, `status`, `usageCount`)
VALUES
('photo', 'ACTIVE', 0),
('video', 'ACTIVE', 0),
('audio', 'ACTIVE', 0),
('document', 'ACTIVE', 0),
('extra link', 'ACTIVE', 0),
('other', 'ACTIVE', 0);

-- 默认资源主题
INSERT INTO `category` (`categoryTopic`, `status`, `usageCount`)
VALUES
('places', 'ACTIVE', 0),
('traditions', 'ACTIVE', 0),
('stories', 'ACTIVE', 0),
('objects', 'ACTIVE', 0),
('educational materials', 'ACTIVE', 0),
('other', 'ACTIVE', 0);

SET FOREIGN_KEY_CHECKS = 1;
