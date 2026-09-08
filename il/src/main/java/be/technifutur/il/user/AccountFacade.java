package be.technifutur.il.user;

import be.technifutur.bll.user.AccountService;
import be.technifutur.dl.user.ChangeEmailRequestDto;
import be.technifutur.dl.user.ChangePasswordRequestDto;
import be.technifutur.dl.user.DeleteAccountRequestDto;
import be.technifutur.dl.user.UserDto;
import org.springframework.stereotype.Component;

/**
 * Les opérations qu'un membre fait sur son propre compte.
 * <p>
 * Séparées du profil : modifier sa biographie et changer son mot de passe n'engagent pas les
 * mêmes précautions, et les mélanger dans une même façade finirait par les mélanger dans un
 * même écran.
 */
@Component
public class AccountFacade {

    private final AccountService accountService;
    private final UserMapper userMapper;

    public AccountFacade(AccountService accountService, UserMapper userMapper) {
        this.accountService = accountService;
        this.userMapper = userMapper;
    }

    public void changePassword(String username, ChangePasswordRequestDto request) {
        accountService.changePassword(username, request.getCurrentPassword(), request.getNewPassword());
    }

    public UserDto changeEmail(String username, ChangeEmailRequestDto request) {
        return userMapper.toDto(
                accountService.changeEmail(username, request.getCurrentPassword(), request.getEmail()));
    }

    public void deleteAccount(String username, DeleteAccountRequestDto request) {
        accountService.deleteAccount(username, request.getCurrentPassword());
    }
}
