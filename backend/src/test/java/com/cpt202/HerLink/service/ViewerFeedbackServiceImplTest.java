package com.cpt202.HerLink.service;

import com.cpt202.HerLink.dto.viewer.FeedbackCreateRequest;
import com.cpt202.HerLink.entity.AttachedFile;
import com.cpt202.HerLink.entity.Feedback;
import com.cpt202.HerLink.exception.AppException;
import com.cpt202.HerLink.mapper.AttachedFileMapper;
import com.cpt202.HerLink.mapper.FeedbackMapper;
import com.cpt202.HerLink.service.impl.ViewerFeedbackServiceImpl;
import com.cpt202.HerLink.util.FileStorageManager;
import com.cpt202.HerLink.vo.FeedbackVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewerFeedbackServiceImplTest {

    @Mock
    private FeedbackMapper feedbackMapper;

    @Mock
    private AttachedFileMapper attachedFileMapper;

    @Mock
    private FileStorageManager fileStorageManager;

    private ViewerFeedbackServiceImpl viewerFeedbackService;

    @BeforeEach
    void setUp() {
        viewerFeedbackService = new ViewerFeedbackServiceImpl(feedbackMapper, attachedFileMapper, fileStorageManager);
    }

    @Test
    void createFeedback_shouldPersistFeedbackAndAttachments() {
        Long currentUserId = 6L;

        MockMultipartFile attachment = new MockMultipartFile(
                "files",
                "viewer-note.png",
                "image/png",
                "png-data".getBytes()
        );

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setFeedbackType("Bug Report");
        request.setDescription("The media preview does not render correctly.");
        request.setFiles(new MockMultipartFile[] { attachment });

        doAnswer(invocation -> {
            Feedback feedback = invocation.getArgument(0);
            feedback.setFeedbackId(44L);
            return 1;
        }).when(feedbackMapper).insert(any(Feedback.class));

        FileStorageManager.StoredFile storedFile = new FileStorageManager.StoredFile();
        storedFile.setOriginalFilename("viewer-note.png");
        storedFile.setStoredFilename("stored-viewer-note.png");
        storedFile.setFilePath("feedback-44/stored-viewer-note.png");
        storedFile.setFileType("png");
        storedFile.setFileSize(attachment.getSize());
        when(fileStorageManager.storeFile(any(), any())).thenReturn(storedFile);

        Feedback savedFeedback = new Feedback();
        savedFeedback.setFeedbackId(44L);
        savedFeedback.setFileNum(1);
        savedFeedback.setUploadedAt(LocalDateTime.now());
        savedFeedback.setUserId(currentUserId);
        savedFeedback.setFeedbackType("Bug Report");
        savedFeedback.setDescription("The media preview does not render correctly.");
        when(feedbackMapper.selectById(44L)).thenReturn(savedFeedback);

        AttachedFile savedAttachment = new AttachedFile();
        savedAttachment.setFileId(101L);
        savedAttachment.setFeedbackId(44L);
        savedAttachment.setOriginalFilename("viewer-note.png");
        savedAttachment.setStoredFilename("stored-viewer-note.png");
        savedAttachment.setFilePath("feedback-44/stored-viewer-note.png");
        savedAttachment.setFileType("PNG");
        savedAttachment.setFileSize(attachment.getSize());
        savedAttachment.setUploadedAt(LocalDateTime.now());
        when(attachedFileMapper.selectByFeedbackId(44L)).thenReturn(List.of(savedAttachment));

        FeedbackVO result = viewerFeedbackService.createFeedback(currentUserId, request);

        assertNotNull(result);
        assertEquals(44L, result.getFeedbackId());
        assertEquals("Bug Report", result.getFeedbackType());
        assertEquals(1, result.getAttachments().size());
        assertEquals("viewer-note.png", result.getAttachments().get(0).getOriginalFilename());
        assertEquals("PNG", result.getAttachments().get(0).getFileType());

        ArgumentCaptor<Feedback> feedbackCaptor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(feedbackCaptor.capture());
        assertEquals(1, feedbackCaptor.getValue().getFileNum());
        assertEquals(currentUserId, feedbackCaptor.getValue().getUserId());

        ArgumentCaptor<AttachedFile> attachmentCaptor = ArgumentCaptor.forClass(AttachedFile.class);
        verify(attachedFileMapper).insert(attachmentCaptor.capture());
        assertEquals(44L, attachmentCaptor.getValue().getFeedbackId());
        assertEquals("viewer-note.png", attachmentCaptor.getValue().getOriginalFilename());
        assertEquals("PNG", attachmentCaptor.getValue().getFileType());
    }

    @Test
    void createFeedback_shouldRejectUnsupportedAttachmentType() {
        MockMultipartFile attachment = new MockMultipartFile(
                "files",
                "viewer-note.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "docx-data".getBytes()
        );

        FeedbackCreateRequest request = new FeedbackCreateRequest();
        request.setFeedbackType("Suggestion");
        request.setDescription("Please add more context for the archive.");
        request.setFiles(new MockMultipartFile[] { attachment });

        AppException exception = assertThrows(
                AppException.class,
                () -> viewerFeedbackService.createFeedback(5L, request)
        );

        assertEquals(400, exception.getStatusCode());
        assertEquals("Feedback attachments must be JPG, PNG, PDF, or TXT.", exception.getMessage());
        verify(feedbackMapper, never()).insert(any(Feedback.class));
    }
}
