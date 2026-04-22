package com.cpt202.HerLink.service.impl;

import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.entity.AttachedFile;
import com.cpt202.HerLink.entity.Feedback;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AttachedFileMapper;
import com.cpt202.HerLink.mapper.FeedbackMapper;
import com.cpt202.HerLink.service.ViewerFeedbackService;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.util.FileTypeValidator;
import com.cpt202.HerLink.vo.AttachedFileVO;
import com.cpt202.HerLink.vo.FeedbackVO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ViewerFeedbackServiceImpl implements ViewerFeedbackService {

    private static final int MAX_ATTACHMENT_COUNT = 3;
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024L * 1024L;

    private final FeedbackMapper feedbackMapper;
    private final AttachedFileMapper attachedFileMapper;
    private final FileStorageManager fileStorageManager;

    public ViewerFeedbackServiceImpl(FeedbackMapper feedbackMapper,
                                     AttachedFileMapper attachedFileMapper,
                                     FileStorageManager fileStorageManager) {
        this.feedbackMapper = feedbackMapper;
        this.attachedFileMapper = attachedFileMapper;
        this.fileStorageManager = fileStorageManager;
    }

    @Override
    @Transactional
    public FeedbackVO createFeedback(Long currentUserId, FeedbackCreateRequest request) {
        String feedbackType = normalizeFeedbackType(request == null ? null : request.getFeedbackType());
        String description = normalizeDescription(request == null ? null : request.getDescription());
        List<MultipartFile> attachments = normalizeAttachments(request == null ? null : request.getFiles());
        validateAttachments(attachments);

        LocalDateTime now = LocalDateTime.now();
        Feedback feedback = new Feedback();
        feedback.setFileNum(attachments.size());
        feedback.setUploadedAt(now);
        feedback.setUserId(currentUserId);
        feedback.setFeedbackType(feedbackType);
        feedback.setDescription(description);
        feedbackMapper.insert(feedback);

        List<String> storedPaths = new ArrayList<>();
        try {
            for (MultipartFile attachment : attachments) {
                FileStorageManager.StoredFile storedFile = fileStorageManager.storeFile(
                        attachment,
                        "feedback-" + feedback.getFeedbackId()
                );
                if (storedFile == null) {
                    continue;
                }

                storedPaths.add(storedFile.getFilePath());
                AttachedFile attachedFile = new AttachedFile();
                attachedFile.setFeedbackId(feedback.getFeedbackId());
                attachedFile.setOriginalFilename(storedFile.getOriginalFilename());
                attachedFile.setStoredFilename(storedFile.getStoredFilename());
                attachedFile.setFilePath(storedFile.getFilePath());
                attachedFile.setFileType(storedFile.getFileType().toUpperCase(Locale.ROOT));
                attachedFile.setFileSize(storedFile.getFileSize());
                attachedFile.setUploadedAt(now);
                attachedFileMapper.insert(attachedFile);
            }
        } catch (RuntimeException exception) {
            for (String storedPath : storedPaths) {
                fileStorageManager.deleteQuietly(storedPath);
            }
            throw exception;
        }

        Feedback savedFeedback = feedbackMapper.selectById(feedback.getFeedbackId());
        if (savedFeedback == null) {
            throw AppException.notFound("Feedback was saved but cannot be reloaded.");
        }

        return buildFeedbackVO(savedFeedback);
    }

    @Override
    public List<FeedbackVO> listMyFeedback(Long currentUserId) {
        List<Feedback> feedbackList = feedbackMapper.selectByUserId(currentUserId);
        if (feedbackList == null) {
            return Collections.emptyList();
        }

        List<FeedbackVO> feedbackVOList = new ArrayList<>();
        for (Feedback feedback : feedbackList) {
            feedbackVOList.add(buildFeedbackVO(feedback));
        }
        return feedbackVOList;
    }

    private String normalizeFeedbackType(String feedbackType) {
        String normalizedType = feedbackType == null ? null : feedbackType.trim();
        if ("Bug Report".equalsIgnoreCase(normalizedType)) {
            return "Bug Report";
        }
        if ("Suggestion".equalsIgnoreCase(normalizedType)) {
            return "Suggestion";
        }
        throw AppException.badRequest("Feedback type must be Bug Report or Suggestion.");
    }

    private String normalizeDescription(String description) {
        String normalizedDescription = description == null ? null : description.trim();
        if (normalizedDescription == null || normalizedDescription.isEmpty()) {
            throw AppException.badRequest("Feedback description is required.");
        }
        return normalizedDescription;
    }

    private List<MultipartFile> normalizeAttachments(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }

        List<MultipartFile> attachments = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            attachments.add(file);
        }
        return attachments;
    }

    private void validateAttachments(List<MultipartFile> attachments) {
        if (attachments.size() > MAX_ATTACHMENT_COUNT) {
            throw AppException.badRequest("You can upload up to 3 feedback attachments.");
        }

        for (MultipartFile attachment : attachments) {
            if (attachment.getSize() > MAX_ATTACHMENT_SIZE) {
                throw AppException.badRequest("Each feedback attachment must be 10MB or smaller.");
            }
            if (!FileTypeValidator.isFeedbackAttachmentSupported(
                    attachment.getOriginalFilename(),
                    attachment.getContentType()
            )) {
                throw AppException.badRequest(
                        "Feedback attachments must be " + FileTypeValidator.describeFeedbackAttachmentTypes() + "."
                );
            }
        }
    }

    private FeedbackVO buildFeedbackVO(Feedback feedback) {
        FeedbackVO feedbackVO = new FeedbackVO();
        feedbackVO.setFeedbackId(feedback.getFeedbackId());
        feedbackVO.setFileNum(feedback.getFileNum());
        feedbackVO.setUploadedAt(feedback.getUploadedAt());
        feedbackVO.setUserId(feedback.getUserId());
        feedbackVO.setFeedbackType(feedback.getFeedbackType());
        feedbackVO.setDescription(feedback.getDescription());
        feedbackVO.setAttachments(buildAttachedFileVOList(attachedFileMapper.selectByFeedbackId(feedback.getFeedbackId())));
        return feedbackVO;
    }

    private List<AttachedFileVO> buildAttachedFileVOList(List<AttachedFile> attachedFiles) {
        if (attachedFiles == null || attachedFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<AttachedFileVO> attachedFileVOList = new ArrayList<>();
        for (AttachedFile attachedFile : attachedFiles) {
            AttachedFileVO attachedFileVO = new AttachedFileVO();
            attachedFileVO.setFileId(attachedFile.getFileId());
            attachedFileVO.setFeedbackId(attachedFile.getFeedbackId());
            attachedFileVO.setOriginalFilename(attachedFile.getOriginalFilename());
            attachedFileVO.setStoredFilename(attachedFile.getStoredFilename());
            attachedFileVO.setFilePath(attachedFile.getFilePath());
            attachedFileVO.setFileType(attachedFile.getFileType());
            attachedFileVO.setFileSize(attachedFile.getFileSize());
            attachedFileVO.setUploadedAt(attachedFile.getUploadedAt());
            attachedFileVOList.add(attachedFileVO);
        }
        return attachedFileVOList;
    }
}
