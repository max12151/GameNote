package be.technifutur.bll.follow;

import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.follow.UserFollowEntity;
import be.technifutur.dal.follow.UserFollowRepository;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserSearchView;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qui suit qui.
 * <p>
 * Suivre quelqu'un ne demande pas son accord et ne crée pas le lien inverse : c'est un
 * abonnement à ce qu'il publie, pas une relation. Les deux opérations sont idempotentes —
 * suivre deux fois ne fait rien de plus, arrêter de suivre quelqu'un qu'on ne suivait pas non
 * plus.
 */
@Service
public class FollowService {

    /** Plafond des listes d'abonnés et d'abonnements servies d'un coup. */
    private static final int MAX_PROFILES = 100;

    private final UserFollowRepository userFollowRepository;
    private final UserRepository userRepository;

    public FollowService(UserFollowRepository userFollowRepository, UserRepository userRepository) {
        this.userFollowRepository = userFollowRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void follow(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new InvalidOperationException("On ne peut pas se suivre soi-même");
        }

        UserEntity followed = userRepository.findById(followedId)
                .orElseThrow(() -> new ResourceNotFoundException("Joueur introuvable"));

        // Anonymisé ou suspendu, ce compte n'a plus de page publique : s'y abonner reviendrait
        // à suivre une adresse qui répond 404. Les abonnements déjà en place, eux, ne sont pas
        // rompus — une suspension peut être levée, et le lien reprend alors sans rien redemander.
        if (!followed.isActive()) {
            throw new ResourceNotFoundException("Joueur introuvable");
        }

        if (userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return;
        }

        UserFollowEntity follow = new UserFollowEntity();
        follow.setFollowerId(followerId);
        follow.setFollowedId(followedId);
        follow.setCreatedAt(OffsetDateTime.now());

        userFollowRepository.save(follow);
    }

    @Transactional
    public void unfollow(Long followerId, Long followedId) {
        userFollowRepository.deleteFollow(followerId, followedId);
    }

    public boolean isFollowing(Long followerId, Long followedId) {
        return followerId != null
                && followedId != null
                && userFollowRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    public List<Long> getFollowedIds(Long followerId) {
        return userFollowRepository.findFollowedIds(followerId);
    }

    /**
     * Parmi ces membres, ceux que l'utilisateur suit déjà. Une requête pour toute une liste
     * de résultats plutôt qu'une question par ligne affichée.
     */
    public Set<Long> getFollowedIdsAmong(Long followerId, Collection<Long> candidateIds) {
        if (followerId == null || candidateIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(userFollowRepository.findFollowedIdsAmong(followerId, candidateIds));
    }

    public long countFollowing(Long userId) {
        return userFollowRepository.countByFollowerId(userId);
    }

    public long countFollowers(Long userId) {
        return userFollowRepository.countByFollowedId(userId);
    }

    public List<UserSearchView> getFollowing(Long userId, int limit) {
        return userFollowRepository.findFollowedProfiles(userId, PageRequest.of(0, clamp(limit)));
    }

    public List<UserSearchView> getFollowers(Long userId, int limit) {
        return userFollowRepository.findFollowerProfiles(userId, PageRequest.of(0, clamp(limit)));
    }

    private static int clamp(int limit) {
        return Math.clamp(limit, 1, MAX_PROFILES);
    }
}
