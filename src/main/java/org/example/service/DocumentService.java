package org.example.service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.CodingErrorAction;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DocumentService {
    private static final Set<String> EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx", "txt", "xlsx", "pptx");
    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final WorkflowRepository workflowRepository;
    private final AccountRepository accountRepository;
    private final Path storageRoot;
    private final long maxBytes;

    public DocumentService(ApplicationService applicationService, ApplicationRepository applicationRepository,
                           WorkflowRepository workflowRepository, AccountRepository accountRepository,
                           @Value("${app.storage.root:./data/uploads}") String storageRoot,
                           @Value("${app.storage.max-file-size:10485760}") long maxBytes) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.workflowRepository = workflowRepository;
        this.accountRepository = accountRepository;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    @Transactional
    public Map<String, Object> upload(long applicationId, Long checklistItemId, MultipartFile file) {
        CustomUserDetails user = CurrentUser.get();
        if (!user.hasRole("USER"))
            throw new org.springframework.security.access.AccessDeniedException("Only applicants can upload application documents.");
        applicationService.ensureApplicantOwns(applicationId);
        String applicationStatus = applicationRepository.status(applicationId);
        if (!"DRAFT".equals(applicationStatus)) {
            boolean correctionWindow = Set.of("SUBMITTED", "UNDER_REVIEW").contains(applicationStatus)
                    && workflowRepository.latestDocumentNeedsCorrection(applicationId, checklistItemId);
            if (!correctionWindow)
                throw new BusinessException("After submission, uploads are allowed only to replace the latest document marked REJECTED or NEEDS_CORRECTION.");
        }
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose a document to upload.");
        if (file.getSize() > maxBytes)
            throw new IllegalArgumentException("The uploaded file exceeds the configured size limit.");
        String original = file.getOriginalFilename() == null ? "document" : file.getOriginalFilename();
        original = original.replace('\\', '/');
        int lastSlash = original.lastIndexOf('/');
        if (lastSlash >= 0) original = original.substring(lastSlash + 1);
        original = original.replaceAll("[\\p{Cntrl}]", "_").trim();
        if (original.isBlank()) original = "document";
        if (original.length() > 255) original = original.substring(original.length() - 255);
        String ext = extension(original);
        if (!EXTENSIONS.contains(ext))
            throw new IllegalArgumentException("Allowed file types: PDF, PNG, JPG, DOC, DOCX, TXT, XLSX and PPTX.");
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        // Browsers may report generic content types for Office files. Signatures and
        // ZIP structure are verified below; never trust the browser-supplied MIME type.
        try {
            byte[] prefix = file.getBytes();
            if (!matchesSignature(prefix, ext))
                throw new IllegalArgumentException("The file content does not match its extension or is not a valid supported document.");
            contentType = contentTypeFor(ext);
            Files.createDirectories(storageRoot);
            String storedName = UUID.randomUUID() + "." + ext;
            Path destination = storageRoot.resolve(storedName).normalize();
            if (!destination.startsWith(storageRoot)) throw new IllegalArgumentException("Invalid file name.");
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            try {
                long documentId = workflowRepository.insertDocument(applicationId, checklistItemId, original,
                        contentType, destination.toString(), user.getUserId());
                return Map.of("documentId", documentId, "fileName", original, "reviewStatus", "PENDING");
            } catch (RuntimeException ex) {
                Files.deleteIfExists(destination);
                throw ex;
            }
        } catch (IOException ex) {
            throw new BusinessException("The document could not be stored.", ex);
        }
    }

    public java.util.List<Map<String, Object>> list(long applicationId) {
        applicationService.ensureCanAccess(applicationId);
        return workflowRepository.documents(applicationId);
    }

    @Transactional
    public void review(long documentId, DocumentReviewRequest body) {
        CustomUserDetails user = CurrentUser.get();
        ApplicationService.requireStaffRole(user);
        Long staffId = accountRepository.staffId(user.getUserId());
        if (staffId == null) throw new BusinessException("An active staff profile is required to review documents.");
        Map<String, Object> doc = workflowRepository.documentStorage(documentId);
        applicationService.ensureCanAccess(((Number) doc.get("applicationId")).longValue());
        String status = String.valueOf(body.getOrDefault("reviewStatus", "")).trim().toUpperCase(Locale.ROOT);
        if (!Set.of("ACCEPTED", "REJECTED", "NEEDS_CORRECTION", "PENDING").contains(status))
            throw new IllegalArgumentException("Invalid document review status.");
        String note = body.get("reviewNote") == null ? null : String.valueOf(body.get("reviewNote")).trim();
        if (note != null && note.length() > 1000)
            throw new IllegalArgumentException("Review note must be at most 1000 characters.");
        if (Set.of("REJECTED", "NEEDS_CORRECTION").contains(status) && (note == null || note.isBlank()))
            throw new IllegalArgumentException("Give a review note explaining the correction required.");
        workflowRepository.reviewDocument(documentId, status, staffId, note);
    }

    public DownloadedDocument download(long documentId) {
        Map<String, Object> doc = workflowRepository.documentStorage(documentId);
        applicationService.ensureCanAccess(((Number) doc.get("applicationId")).longValue());
        Path file = Path.of(String.valueOf(doc.get("filePath"))).toAbsolutePath().normalize();
        if (!file.startsWith(storageRoot) || !Files.isRegularFile(file))
            throw new ResourceNotFoundException("Stored document is unavailable.");
        String type = doc.get("fileType") == null ? "application/octet-stream" : String.valueOf(doc.get("fileType"));
        return new DownloadedDocument(new FileSystemResource(file), String.valueOf(doc.get("fileName")), type);
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static boolean matchesSignature(byte[] bytes, String ext) {
        if (bytes.length < 4) return false;
        return switch (ext) {
            case "pdf" -> bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F';
            case "png" -> bytes.length > 7 && (bytes[0] & 0xff) == 0x89 &&
                    bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G' &&
                    bytes[4] == 13 && bytes[5] == 10 && bytes[6] == 26 && bytes[7] == 10;
            case "jpg", "jpeg" -> (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
            case "doc" -> bytes.length >= 8 && (bytes[0] & 0xff) == 0xd0 && (bytes[1] & 0xff) == 0xcf &&
                    (bytes[2] & 0xff) == 0x11 && (bytes[3] & 0xff) == 0xe0 && (bytes[4] & 0xff) == 0xa1 &&
                    (bytes[5] & 0xff) == 0xb1 && (bytes[6] & 0xff) == 0x1a && (bytes[7] & 0xff) == 0xe1;
            case "docx" -> isOfficeZip(bytes, "word/document.xml");
            case "xlsx" -> isOfficeZip(bytes, "xl/workbook.xml");
            case "pptx" -> isOfficeZip(bytes, "ppt/presentation.xml");
            case "txt" -> isUtf8Text(bytes);
            default -> false;
        };
    }

    private static boolean isOfficeZip(byte[] bytes, String requiredEntry) {
        if (bytes[0] != 'P' || bytes[1] != 'K') return false;
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            int count = 0;
            long totalExpanded = 0;
            boolean found = false;
            byte[] buffer = new byte[4096];
            while ((entry = in.getNextEntry()) != null) {
                if (++count > 2000) return false;
                if (entry.getName().equals(requiredEntry)) found = true;
                int n;
                while ((n = in.read(buffer)) != -1) {
                    totalExpanded += n;
                    if (totalExpanded > 30L * 1024 * 1024) return false;
                }
                in.closeEntry();
            }
            return found;
        } catch (IOException ex) {
            return false;
        }
    }

    private static boolean isUtf8Text(byte[] bytes) {
        for (byte b : bytes) if (b == 0) return false;
        try {
            StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes));
            return true;
        } catch (CharacterCodingException ex) {
            return false;
        }
    }

    private static String contentTypeFor(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }

    public record DownloadedDocument(Resource resource, String fileName, String contentType) {
    }
}

