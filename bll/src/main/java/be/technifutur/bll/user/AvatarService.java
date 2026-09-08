package be.technifutur.bll.user;

import be.technifutur.dal.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Décode les avatars stockés en data URI base64 pour qu'ils puissent être servis comme des
 * images ordinaires, cachables par le navigateur, plutôt que recopiés en base64 dans chaque
 * réponse JSON qui mentionne leur auteur.
 */
@Service
public class AvatarService {

    private static final String DATA_PREFIX = "data:";
    private static final String BASE64_MARKER = ";base64";

    /**
     * Liste blanche stricte. Le type MIME provient d'une chaîne fournie par l'utilisateur :
     * le renvoyer sans contrôle laisserait servir du {@code text/html} depuis le domaine de
     * l'API, donc exécuter du script dans son origine.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif", "image/avif");

    private final UserRepository userRepository;

    public AvatarService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<AvatarImage> findAvatar(Long userId) {
        return userRepository.findAvatarUrlById(userId)
                .filter(dataUri -> !dataUri.isBlank())
                .flatMap(AvatarService::decode);
    }

    private static Optional<AvatarImage> decode(String dataUri) {
        if (!dataUri.startsWith(DATA_PREFIX)) {
            return Optional.empty();
        }

        int separator = dataUri.indexOf(',');
        if (separator < 0) {
            return Optional.empty();
        }

        String metadata = dataUri.substring(DATA_PREFIX.length(), separator);
        if (!metadata.endsWith(BASE64_MARKER)) {
            return Optional.empty();
        }

        String contentType = metadata.substring(0, metadata.length() - BASE64_MARKER.length())
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return Optional.empty();
        }

        byte[] data;
        try {
            data = Base64.getDecoder().decode(dataUri.substring(separator + 1));
        } catch (IllegalArgumentException malformedBase64) {
            return Optional.empty();
        }

        return Optional.of(new AvatarImage(data, contentType, etagOf(data)));
    }

    private static String etagOf(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            // 16 caractères hexadécimaux suffisent largement à distinguer deux avatars.
            return '"' + HexFormat.of().formatHex(digest, 0, 8) + '"';
        } catch (NoSuchAlgorithmException impossible) {
            // SHA-256 fait partie des algorithmes que toute JVM doit fournir.
            return '"' + Integer.toHexString(new String(data, StandardCharsets.ISO_8859_1).hashCode()) + '"';
        }
    }
}
