package com.emar.order_app.customer;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;

@Service
public class CustomerFileService {

    private final CustomerRepository customerRepository;
    private final CustomerFileRepository fileRepository;

    public CustomerFileService(CustomerRepository customerRepository, CustomerFileRepository fileRepository) {
        this.customerRepository = customerRepository;
        this.fileRepository = fileRepository;
    }

    public List<CustomerFileEntity> listForCustomer(Long customerId) {
        return fileRepository.findByCustomerIdOrderByUploadedAtDescIdDesc(customerId);
    }

    @Transactional
    public CustomerFileEntity upload(Long customerId, MultipartFile file, String uploadedByUsername) throws IOException {
        if (customerId == null) throw new IllegalArgumentException("customerId zorunlu");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("file zorunlu");

        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) originalName = "file";
        // very small sanitization
        originalName = originalName.replace("..", ".").replace("/", "_").replace("\\", "_");

        // Default: store under user home to avoid permission issues depending on JVM working dir
        String home = System.getProperty("user.home", ".");
        Path baseDir = Path.of(home, ".mrcrm", "uploads", "customers", String.valueOf(customerId));
        Files.createDirectories(baseDir);

        String storedName = Instant.now().toEpochMilli() + "_" + originalName;
        Path target = baseDir.resolve(storedName);

        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        CustomerFileEntity e = new CustomerFileEntity();
        e.setCustomer(customer);
        e.setFileName(originalName);
        e.setContentType(file.getContentType());
        e.setFileSize(file.getSize());
        e.setStoragePath(target.toString());
        e.setUploadedByUsername(uploadedByUsername);

        return fileRepository.save(e);
    }

    public Resource loadAsResource(Long customerId, Long fileId) {
        CustomerFileEntity e = fileRepository.findByIdAndCustomerId(fileId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
        return new FileSystemResource(e.getStoragePath());
    }

    public CustomerFileEntity getMeta(Long customerId, Long fileId) {
        return fileRepository.findByIdAndCustomerId(fileId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));
    }

    @Transactional
    public void delete(Long customerId, Long fileId) throws IOException {
        if (customerId == null) throw new IllegalArgumentException("customerId zorunlu");
        if (fileId == null) throw new IllegalArgumentException("fileId zorunlu");

        CustomerFileEntity e = fileRepository.findByIdAndCustomerId(fileId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("File not found"));

        if (e.getStoragePath() != null && !e.getStoragePath().isBlank()) {
            try {
                Files.deleteIfExists(Path.of(e.getStoragePath()));
            } catch (Exception ignore) {
            }
        }

        fileRepository.delete(e);
    }
}
