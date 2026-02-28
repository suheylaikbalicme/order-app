package com.emar.order_app.auth;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Set;

@Controller
public class ProfileController {

    private static final long MAX_AVATAR_BYTES = 10L * 1024L * 1024L; // 10MB
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model,
                          @RequestParam(value = "ok", required = false) String ok,
                          @RequestParam(value = "err", required = false) String err) {
        String username = authentication.getName();
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));

        model.addAttribute("user", user);
        if (ok != null) model.addAttribute("successMessage", ok);
        if (err != null) model.addAttribute("errorMessage", err);
        return "profile";
    }

    @PostMapping("/profile/avatar")
    public String uploadAvatar(Authentication authentication,
                               @RequestParam("avatar") MultipartFile avatarFile) {
        String username = authentication.getName();
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));

        if (avatarFile == null || avatarFile.isEmpty()) {
            return "redirect:/profile?err=" + url("Lütfen bir görsel dosyası seçin.");
        }

        if (avatarFile.getSize() > MAX_AVATAR_BYTES) {
            return "redirect:/profile?err=" + url("Dosya boyutu 10MB'dan büyük olamaz.");
        }

        String contentType = avatarFile.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return "redirect:/profile?err=" + url("Sadece JPG, PNG veya WEBP dosyaları kabul edilir.");
        }

        try {
            user.setAvatarData(avatarFile.getBytes());
            user.setAvatarContentType(contentType);
            user.setAvatarUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            return "redirect:/profile?ok=" + url("Profil fotoğrafı güncellendi.");
        } catch (Exception e) {
            return "redirect:/profile?err=" + url("Fotoğraf yüklenemedi. Lütfen tekrar deneyin.");
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(Authentication authentication,
                                 @RequestParam("currentPassword") String currentPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("newPassword2") String newPassword2) {
        String username = authentication.getName();
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));

        if (newPassword == null || newPassword.length() < 6) {
            return "redirect:/profile?err=" + url("Yeni şifre en az 6 karakter olmalıdır.");
        }
        if (!newPassword.equals(newPassword2)) {
            return "redirect:/profile?err=" + url("Yeni şifreler eşleşmiyor.");
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "redirect:/profile?err=" + url("Mevcut şifre yanlış.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return "redirect:/profile?ok=" + url("Şifren güncellendi.");
    }

    @GetMapping("/avatars/me")
    public ResponseEntity<byte[]> myAvatar(Authentication authentication) {
        String username = authentication.getName();
        UserEntity user = userRepository.findByUsernameIgnoreCase(username)
                .orElse(null);
        if (user == null || user.getAvatarData() == null || user.getAvatarData().length == 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String ct = user.getAvatarContentType();
        if (ct == null || ct.isBlank()) ct = MediaType.IMAGE_PNG_VALUE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(ct));
        headers.setCacheControl(CacheControl.noCache().getHeaderValue());
        return new ResponseEntity<>(user.getAvatarData(), headers, HttpStatus.OK);
    }

    private static String url(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
