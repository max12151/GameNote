package be.technifutur.bll.user;

/**
 * Avatar décodé, prêt à être servi comme une vraie image.
 *
 * @param contentType type MIME, restreint à une liste blanche d'images : la valeur vient
 *                    d'une data URI fournie par l'utilisateur, la renvoyer telle quelle
 *                    permettrait de faire servir du HTML par le domaine de l'API
 * @param etag        empreinte du contenu, pour que le navigateur revalide sans retélécharger
 */
public record AvatarImage(byte[] data, String contentType, String etag) {
}
